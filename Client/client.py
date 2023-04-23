from kazoo.client import KazooClient, DataWatch
from functools import partial
from kazoo.protocol.states import ZnodeStat, WatchedEvent, EventType
import requests
import threading
import json

class Buffer:
    def __init__(self,zk, node_path, lock) -> None:
        # 储存所有的region节点，主要是跟zookeeper进行同步
        self.region_names: list[str] = []
        # 储存 table_name -> region_name 的映射
        self.table_region_map: dict[str, str] = dict()
        # 储存 region_name -> hosts+port 的映射
        self.region_hosts_map: dict[str, str] = dict()
        
        # 储存 region_name -> region_watcher 的映射
        self.region_watchers: dict[str, partial[DataWatch]] = dict()
        # lss节点下children_list的监听器
        self.region_list_watcher = None
        
        # 线程安全锁
        self.lock = lock
        
        self.zk: KazooClient = zk
        self.node_path = node_path
        
        # 初始化Buffer
        print("Buffer init...")
        self.refresh_buffer()
    
    
    def get_region_url(self, table_name) -> str | None:
        with self.lock:
            if table_name in self.table_region_map:
                region_name = self.table_region_map[table_name]
                if region_name not in self.region_hosts_map:
                    # 这种情况理应不存在
                    print("WARNING: region found in table_region_map but not found in region_hosts_name.")
                    print("Buffer will be refreshed")
                    # 将本函数已经获得锁传给refresh_buffer,避免refresh_buffer内部继续请求self.lock造成死锁
                    self.refresh_buffer(self.lock)
                return self.region_hosts_map[region_name]
            else: return None 
    
    def get_region_data(self, region_name):
        try:
            data:bytes = self.zk.get(self.node_path+'/'+region_name)[0]
            return data.decode()
        except Exception as e:
            print("get_region_data Error", e)
    
    # 开启region节点列表监听
    def open_region_list_watcher(self):
        try:
            self.region_list_watcher = self.zk.ChildrenWatch(self.node_path,self.region_list_changed)
        except Exception as e:
            print("children_list_watcher Error:", e)
    
    
    # 开启某个region的监视器
    def open_region_data_watcher(self, region_name):
        try:
            region_watcher = self.zk.DataWatch(self.node_path+'/'+region_name,partial(self.region_data_changed, region_name))
            with self.lock:
                self.region_watchers[region_name] = region_watcher
        except Exception as e:
            print("region_data_watcher Error:", e)
             
             
    # 重置整个buffer    
    def refresh_buffer(self, lock=None):
        # 获得线程安全锁
        lock = self.lock if lock == None else lock
        try:
            with lock:
                self.region_names.clear()
                self.table_region_map.clear()
                self.region_hosts_map.clear()
                        
                self.region_names = self.zk.get_children(self.node_path)
                for region_name in self.region_names:
                    self.__unsave_append_region(region_name)
                # 清除所有的watcher
                # 根据chatGPT,直接调用该函数即可停止该监视器
                try:
                    self.region_list_watcher()
                except Exception as e:
                    print("clear region_list_watcher Error", e)
                    
                for watch in self.region_watchers:
                    try:
                        watch()
                    except Exception as e:
                        print("clear region_watcher Error", e)   
                self.region_list_watcher = None
                self.region_watchers.clear()
                    
            # 重新打开所有的watcher    
            self.open_region_list_watcher()
            for region_name in self.region_names:
                self.open_region_data_watcher(region_name)
        except Exception as e:
            print("refresh_buffer Error",e)
    
    
    # 在整个缓存中删除有关该region的所有信息
    def delete_region(self, region_name):
        with self.lock:
            self.__unsafe_delete_region(region_name)
    
    
    # 在整个缓存中新增一个region
    def append_region(self, region_name):
        with self.lock:
            self.__unsafe_append_region(region_name)
    
    
    # WARNING: 该方法并不是线程安全的,请在线程安全的条件下调用该函数
    # 在整个缓存中删除有关该region的所有信息
    def __unsafe_delete_region(self, region_name):
        try:
            if self.region_watchers[region_name]:
                # 根据chatGPT,直接调用该函数即可停止该监视器
                self.region_watchers[region_name]()
                del self.region_watchers[region_name]
                
            if self.region_hosts_map[region_name]:
                del self.region_hosts_map[region_name]
            
            # 删除table_region_map中value是region_name的键值对
            self.table_region_map = {k: v for k, v in self.table_region_map.items() if v != region_name}  
                     
        except Exception as e:
            print("delete_region Error",e)
    
    
    # WARNING: 该方法并不是线程安全的,请在线程安全的条件下调用该函数
    # 在整个缓存中新增一个region
    def __unsafe_append_region(self, region_name):
        # 返回table_list里面的master_table,即不以_slave结尾的table
        def get_master_table(table_list: list[str]) -> list[str]:
            result: list[str] = []
            for table in table_list:
                if not table.endswith("_slave"):
                    result.append(table)
            return result
        
        data = self.get_region_data(region_name)
        data_arr = data.split(",")
        hosts = data_arr[0]
        port = data_arr[2]
        
        self.region_hosts_map[region_name] = hosts+":"+port
        
        if len(data_arr) > 4:
            master_tables = get_master_table(data_arr[4:])
            for master_table in master_tables:
                self.table_region_map[master_table] = region_name
        
    
    # region_list_watcher的回调函数,查看是否有节点被删除或新增
    # 由于整个函数已经保证了线程安全,因此在内部使用__unsave函数
    def region_list_changed(self, region_list: list[str]):      
        with self.lock:
            self.region_names = region_list
            # 在缓存中删除已经失效的节点
            for region in self.region_names:
                if region not in region_list:
                    self.__unsafe_delete_region(region)
                    
            # 在缓存中新增节点
            # 由于region_hosts_name已经发生更改，需要重新获取keys
            for region in region_list:
                if region not in self.region_names:
                    self.__unsafe_append_region(region)
                    
        # 原监视器在触发回调后会自动结束，因此我们需要重新起一个监视器
        self.open_region_list_watcher()
                    
                    
    # region_data_watcher的回调函数,只处理CHANGE事件,删除等事件由region_list_watcher来处理
    def region_data_changed(self,region_name: str, data: bytes, stat: ZnodeStat, event: WatchedEvent):
        with self.lock:
            if event.type == EventType.CHANGED:
                data_str = data.decode()
                data_arr = data_str.split(",")
                
                hosts = data_arr[0]
                port = data_arr[2]
                self.region_hosts_map[region_name] = hosts+":"+port
                
                # 如果region里面存了表，则更新
                if len(data_arr) > 4:
                    tables = data_arr[4:]
                    for table_name in tables:
                        if not table_name.endswith("_slave"):
                            self.table_region_map[table_name] = region_name      
                              
        # 原监视器在触发回调后会自动结束，因此我们需要重新起一个监视器
        self.open_region_data_watcher(region_name)
            
        
        
            
class Client:
    def __init__(self, zk_hosts):
        self.zk = KazooClient(hosts=zk_hosts)
        # 查看zookeeper状态
        self.zk.add_listener(lambda state: print('ZooKeeper connection state:', state))
        self.zk.start()
        # 新建一个线程锁，保证缓存的安全
        self.lock = threading.Lock()
        self.node_path = '/curator/lss'
        
        # Buffer管理类
        self.buffer = Buffer(self.zk,self.node_path, self.lock)
        
    # TODO: 使用sql解析库来获取table_name/查询各个region的负载状况并做负载均衡/正常拿到response的处理    
    def exec(sql):
        url = Buffer.get_region_url()
        try_times = 3
        while try_times != 0:
            try:
                response = requests.post(url, data = json.dumps({"sql":sql}), timeout=4)
                break
            except requests.exceptions.Timeout:
                try_times = try_times - 1
                print("sql exec timeout, remain try: ", try_times)
        if try_times == 0:
            print("Error: Max try time!")
            return None
        # 正常拿到了response
        pass

    def close(self):
        self.zk.close()


def print_help():
    print("\n----------Help---------------")
    print("input 'q' or 'Q' to quit")
    print("input 'h' for help")
    print("input others to exec sql")
    print("-----------------------------\n")

if __name__ == '__main__':
    # 自定义zookeeper_hosts
    client = Client('127.0.0.1:2181')
    print("Welcome!\n")
    print_help()
    while True:
        cmd = input("cmd")
        if cmd.lower() == 'h' or cmd.lower() == 'help':
            print_help()
        elif cmd.lower() == 'q':
            client.close()
            print("Bye!")
            break
        else:
            client.exec(cmd)