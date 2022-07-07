import threading
import socket
import math
import time

person_list = []

station_location = [
    [32.204889834211325, 118.72205019609808],  # 气象楼
    [32.205173963507434, 118.72412708389605],  # 晖园宿舍
    [32.20528735885596, 118.72526331783759],  # 雷丁
    [32.20483492924057, 118.72093775749222],  # 文德楼
    [32.204461132982914, 118.71856994659764],  # 明德楼
    [32.201530407707985, 118.71142711844395],  # 逸夫楼
    [32.20214128687547, 118.71322628298861],  # 图书馆
    [32.202648344410605, 118.71637313513355]]  # 中院食堂


class TcpServer:

    def __init__(self, ip):
        self.ip = ip
        self.person = 0
        self.tcp_server = socket.socket()
        self.tcp_server.bind((self.ip, 8080))
        self.next_station = 0
        self.lat = 0
        self.lon = 0
        self.bus_fee = "0"
        self.bus_name = "bus"
        self.bus_seats = "0"

    def listenPort(self):
        flag = True
        i1 = 0
        i2 = 0
        i3 = 0

        self.tcp_server.listen(5)
        self.con, address = self.tcp_server.accept()

        # 判断连接客户端
        recv_data = str(self.con.recv(1024), encoding="utf-8")
        if recv_data == "main":
            self.sendMessage2main(">> 监听程序已启动")
            self.sendMessage2main(">> 端口监听中...")
            while flag:
                # 监听端口
                self.tcp_server.listen(5)
                self.con1, address = self.tcp_server.accept()
                # self.sendMessage2main(f"已与{address}建立连接")

                # 判断连接客户端
                recv_data = str(self.con1.recv(1024), encoding="utf-8")
                print(recv_data)

                if ("BUS" in recv_data):
                    self.con_bus, address_bus = self.con1, address
                    self.sendMessage2main(">> 视频采集端 已连接")
                    i1 += 1

                    # 获取车辆基本参数
                    data = recv_data.split(",")
                    self.bus_fee = data[1]
                    self.bus_name = data[2]
                    self.bus_seats = data[3]

                if (recv_data == "USER"):
                    self.con_user, address_user = self.con1, address
                    self.sendMessage2main(">> 用户端 已连接")
                    i2 += 1

                if (recv_data == "YOLO" and i3 == 0):
                    self.con_yolo, address_yolo = self.con1, address
                    self.sendMessage2main(">> 人脸识别程序 已连接")
                    i3 += 1

                # 全部连接则开始转发
                if (i1 > 0 and i2 > 0 and i3 > 0):
                    self.sendMessage2main(">> 所有设备已就绪")
                    flag = False

    def sendMessage(self, message):
        self.con_user.send(message.encode(encoding='utf-8'))

    def sendMessage2main(self, message):
        self.con.send(message.encode(encoding='utf-8'))

    def retrainsmitLoaction(self):
        while True:
            # 会阻塞,等待新消息
            recv_data = str(self.con_bus.recv(1024), encoding="utf-8")

            # 数据处理
            GPS_info_list = recv_data.split(",")

            # 坐标转换,并判断数据是否失真
            try:

                self.lat = float(GPS_info_list[0])
                self.lon = float(GPS_info_list[1])

                seats_left = int(self.bus_seats) - int(self.person)

                # 纬度 经度 人数 下一站 费用 车名
                send_data = str(self.lat) + "," + \
                            str(self.lon) + "," + \
                            str(seats_left) + "," + \
                            str(self.next_station) + "," + \
                            str(self.bus_fee) + "," + \
                            str(self.bus_name) + ","

                self.sendMessage(send_data)

            except:
                # self.sendMessage2main("发生错误,进入下一次循环")
                pass

    def retrainsmitPerson(self):
        while True:
            # 会阻塞,等待Yolo识别发送的消息
            recv_data = str(self.con_yolo.recv(1024), encoding="utf-8")

            # 接收数据,计算当前人数
            person_now = recv_data
            try:
                person_now = int(float(person_now))  # 格式转化
                person_list.append(person_now)
            except:
                # self.sendMessage2main("读数错误,跳过")
                pass

            # 计算20次结果的平均数
            if (len(person_list) > 20):
                sum = 0
                for i in person_list:
                    sum += i
                    self.person = int(sum / 20 + 0.5)  # 四舍五入
                person_list.clear()

    # 计算车辆航向
    def calc_direction(self):
        while True:
            # 计算最近的三个站点
            station1, station2, station3, dist1, dist2 = find_close_station(self.lat, self.lon)
            time.sleep(2)
            station1, station2, statiion3, dist1_next, dist2_next = find_close_station(self.lat, self.lon)

            # 进行航向判定
            if dist1_next <= dist1 and dist2_next >= dist2:
                next_station = station1
            elif dist1_next <= dist1 and dist2_next >= dist2:
                next_station = station2
            elif dist1_next >= dist1 and dist2_next > dist2:
                next_station = station3
            elif dist1_next <= dist1 and dist2_next <= dist2:
                next_station = station3

            self.next_station = next_station


# 查找最近站点
def find_close_station(lat_bus, lon_bus):
    # 寻找最近的点
    distance_close1 = 100000
    postion_index_close1 = 0
    index1 = 0
    for positon in station_location:
        distance = math.sqrt((lat_bus - positon[0]) ** 2 + (lon_bus - positon[1]) ** 2)
        if distance < distance_close1:
            distance_close1 = distance
            postion_index_close1 = index1
        index1 += 1

    # 寻找第二近的点
    distance_close2 = 100000
    postion_index_close2 = 0
    index2 = 0
    for positon in station_location:
        distance = math.sqrt((lat_bus - positon[0]) ** 2 + (lon_bus - positon[1]) ** 2)
        if distance < distance_close2 and index2 != (postion_index_close1):
            distance_close2 = distance
            postion_index_close2 = index2
        index2 += 1

    # 寻找第三近的点
    distance_close3 = 100000
    postion_index_close3 = 0
    index3 = 0
    for positon in station_location:
        distance = math.sqrt((lat_bus - positon[0]) ** 2 + (lon_bus - positon[1]) ** 2)
        if distance < distance_close3 and index3 != postion_index_close1 and index3 != postion_index_close2:
            distance_close3 = distance
            postion_index_close3 = index3
        index3 += 1

    return postion_index_close1, postion_index_close2, postion_index_close3, distance_close1, distance_close2


def start_TCP_service():
    print("TCP消息转发服务器启动")

    threads = []
    # 获取ip地址
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = str(s.getsockname()[0])

    server = TcpServer(ip)
    server.listenPort()

    # 接收消息
    th1 = threading.Thread(target=server.retrainsmitPerson)
    th1.setDaemon(True)
    th1.start()
    threads.append(th1)
    print("人数转发 执行中")

    # 实现地址转发
    th2 = threading.Thread(target=server.retrainsmitLoaction)
    th2.setDaemon(True)
    th2.start()
    threads.append(th2)
    print("车辆地址转发 执行中")

    th3 = threading.Thread(target=server.calc_direction)
    th3.setDaemon(True)
    th3.start()
    threads.append(th3)
    print("计算站点 执行中")

    # 等待线程运行完毕
    for th in threads:
        th.join()


start_TCP_service()
