import sys
import time
from socket import *
cnt = 0
s = socket(AF_INET, SOCK_DGRAM)

start = time.time()
with open(sys.argv[-1]) as f:
    for line in f.readlines():
        b = line.encode('ascii')
        print('sending: ' + str(b).rstrip())
        s.sendto(line.encode('ascii'), ('localhost', 10666))
        cnt = cnt + 1
        time.sleep(0.0003)

finish = time.time()
print("SENT " + str(cnt) + " events [" + str(cnt/(finish-start)) + " ev/s]")
