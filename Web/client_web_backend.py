import flask
from flask_cors import CORS
import client
import random

app = flask.Flask(__name__)
CORS(app, resources=r'/*')


client_list = []

def clear_list():
    for client in client_list:
        if not client.is_running:
            client_list.remove(client)
        del client

def get_client(uid):
    for client in client_list:
        if client.uid == uid:
            return client
    return None


class WebClient:

    def __init__(self):
        self.client_end = client.Client('120.26.195.57:2181')
        self.is_running = True
        # uid is set to be a random number
        self.uid = random.randint(0, 1000000)
        client_list.append(self)

    def execute(self, msg):
        self.client_end.send(msg)
        return 

    def free(self):
        self.client_end.close()
        self.is_running = False
    



@app.route('/', methods=['GET', 'POST'])
def get_message():

    print("get messsage")
    # print(flask.request.json["msg"])

    return "it is the response"

@app.route('/getbuffer', methods=['POST'])
def get_str():
    print("get buffer")
    print(flask.request.json["uid"])
    return get_client(flask.request.json["uid"]).client_end.get_buffer_str()


@app.route('/getmsg', methods=[ 'GET', 'POST'])
def get_msg():
    print(int(flask.request.json["uid"]))

    temp_client = get_client(int(flask.request.json["uid"])).client_end
    exec_res = temp_client.exec_non_print(flask.request.json["msg"])
    print(exec_res)
    res_dict = {}
    res_dict["res"] = exec_res[0]
    res_dict["log"] = exec_res[1]
    return res_dict

@app.route('/getquit', methods=[ 'GET', 'POST'])
def get_quit():

    get_client(flask.request.json["uid"]).free()
    clear_list()

    return "quit success"

@app.route('/getinit', methods=[ 'GET', 'POST'])
def get_init():
    # uid = random.randint(0, 1000000)
    wc = WebClient()

    return str(wc.uid)

if __name__ == '__main__':
    app.run(debug=True, host="10.162.5.83", port=3003)

