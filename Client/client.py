from kazoo.client import KazooClient
from kazoo.recipe.watchers import ChildrenWatch, DataWatch
from functools import partial
from kazoo.protocol.states import ZnodeStat, WatchedEvent, EventType
import requests
import threading
import json
import sql_metadata

class Buffer:
    def __init__(self,zk, node_path, lock) -> None:
        # 储存所有的region节点，主要是跟zookeeper进行同步
        self.region_names: list[str] = []
        # 储存 table_name -> region_name 的映射
        self.table_region_map: dict[str, str] = dict()
        # 储存 region_name -> hosts+port 的映射
        self.region_hosts_map: dict[str, str] = dict()
        
        # 储存 region_name -> region_watcher 的映射
        self.region_watchers: dict[str, DataWatch] = dict()
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
        lock = self.lock if lock == None else lock
        with lock:
            if table_name in self.table_region_map:
                region_name = self.table_region_map[table_name]
                if region_name not in self.region_hosts_map:
                    # 这种情况理应不存在
                    print("WARNING: region found in table_region_map but not found in region_hosts_name.")
                    print("INFO: Buffer will be refreshed")
                    # 将本函数已经获得锁传给refresh_buffer,避免refresh_buffer内部继续请求self.lock造成死锁
                    self.refresh_buffer(self.lock)
                return self.region_hosts_map[region_name]
            else: return None
    
    def get_region_data(self, region_name):
        try:
            data:bytes = self.zk.get(self.node_path+'/'+region_name)[0]
            return data.decode()
        except Exception as e:
            print("ERROR: get_region_data Error", e)
    
    # 开启region节点列表监听
    def open_region_list_watcher(self):
        try:
            self.region_list_watcher = ChildrenWatch(self.zk,self.node_path,self.region_list_changed)
        except Exception as e:
            print("ERROR: children_list_watcher Error:", e)
    
    
    # 开启某个region的监视器
    def open_region_data_watcher(self, region_name):
        try:
            region_watcher = DataWatch(self.zk,self.node_path+'/'+region_name,partial(self.region_data_changed, region_name))
            with self.lock:
                self.region_watchers[region_name] = region_watcher
        except Exception as e:
            print("ERROR: region_data_watcher Error:", e)
             
             
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
                    self.__unsafe_append_region(region_name)
                # 清除所有的watcher
                # 根据https://stackoverflow.com/questions/40153340/how-to-stop-datawatch-on-zookeeper-node-using-kazoo
                # 直接设置私有变量应该可以手动清除这个监视器
                try:
                    if self.region_list_watcher: self.region_list_watcher._stopped = True
                except Exception as e:
                    print("ERROR: clear region_list_watcher Error", e)
                    
                for watch in self.region_watchers.values():
                    try:
                        watch._stopped = True
                    except Exception as e:
                        print("ERROR: clear region_watcher Error", e)   
                self.region_list_watcher = None
                self.region_watchers.clear()
                    
            # 重新打开所有的watcher    
            self.open_region_list_watcher()
            for region_name in self.region_names:
                self.open_region_data_watcher(region_name)
        except Exception as e:
            print("ERROR: refresh_buffer Error",e)
    
    
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
            print("ERROR: delete_region ERROR",e)
    
    
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
        self.zk.start()
        # 查看zookeeper状态
        self.zk.add_listener(lambda state: print('INFO: ZooKeeper connection state:', state))
        # 新建一个线程锁，保证缓存的安全
        self.lock = threading.Lock()
        self.node_path = '/curator/lss'
        
        # Buffer管理类
        self.buffer = Buffer(self.zk,self.node_path, self.lock)
    
    
    # 暂时不支持多表操作，因此只返回解析出的第一张表    
    def get_table_from_sql(self, sql: str) -> list[str]:
        return sql_metadata.Parser(sql).tables[0]
    
    
    # 检查sql是否为create命令
    def is_create_sql(self, sql: str) -> bool:
        return str(sql_metadata.Parser(sql).tokens[0]).lower() == "create"
    
    
    def get_region_data(self, region_name):
        try:
            data:bytes = self.zk.get(self.node_path+'/'+region_name)[0]
            return data.decode()
        except Exception as e:
            print("ERROR: get_region_data Error", e)
    
    
    # 负载均衡,从zk中查询并返回表最少的region的url
    def get_least_table_region_url(self) -> str:
        try: 
            # 从zk获得region的信息并统计
            regions: list[str] = self.zk.get_children(self.node_path)
            region_data_map: dict[str, int] = dict()
            for region in regions:
                data = self.get_region_data(region)
                region_data_map[region] = data.split(",")
            # 返回最长（即包含table最多）的region的url
            max_region = max(region_data_map, key=lambda k: len(region_data_map[k]))
            # 返回 hosts:port
            return region_data_map[0]+":"+region_data_map[2]        
        except Exception as e:
            print("ERROR: get_least_table_region_url Error",e)
        
        
    # TODO: 格式化输出select    
    def exec(self, sql):
        if self.is_create_sql(sql): # create命令,需要根据负载均衡找到所需的region_url
            url = self.get_least_table_region() 
        else:   # 非create命令,查找对应的table所在的region
            table_name = self.get_table_from_sql(sql)
            url = Buffer.get_region_url(table_name)
            # 检查Buffer内是否含有该table
            if url == None:
                print("INFO: table_name not found in buffer!")
                print("INFO: Buffer will be refreshed!")
                self.buffer.refresh_buffer()
                url = Buffer.get_region_url(table_name)
                if url == None:
                    print("WARNING: table_name not found in refreshed buffer!")
                    print("WARNING: Please check your sql!")
                    return
                
        # 请求region超时后的最大尝试次数
        try_times = 3
        response: requests.Response = None
        while try_times != 0:
            try:
                response: requests.Response = requests.post(url, data = json.dumps({"sql":sql}), timeout=4)
                break
            except requests.exceptions.Timeout:
                try_times = try_times - 1
                print("INFO: sql exec timeout, remain try: ", try_times)
        if try_times == 0:
            print("Error: Max try time!")
            return None
        # 正常拿到了response
        if response.ok():
            result = response.json()
            if result["statusCode"].lower() != "success":
                print("WARNING: sql exec failed!")
                print(result["msg"])
            else:
                if result["type"].lower() != "select":
                    print("Info: sql exec success!")
                    print(result["msg"])
                else:
                    # 格式化输出select
                    pass
        else:
            print("WARNING: response not OK!")
            print(response.json())

    def close(self):
        self.zk.stop()
        self.zk.close()

def print_help():
    print("----------Help---------------")
    print("input 'q' or 'Q' to quit")
    print("input 'h' for help")
    print("input others to exec sql")
    print("-----------------------------")

if __name__ == '__main__':
    # 自定义zookeeper_hosts
    client = Client('120.26.195.57:2181')
    print("Welcome!\n")
    print_help()
    while True:
        cmd = input("\ncmd:")
        if cmd.lower() == 'h' or cmd.lower() == 'help':
            print_help()
        elif cmd.lower() == 'q':
            client.close()
            print("Bye!")
            break
        else:
            client.exec(cmd)