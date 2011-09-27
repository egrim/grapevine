#! /usr/bin/python
import os
import re
import pprint

RE_EXPNUM = re.compile("Exp(\d+)-")
RE_RECEIVED = re.compile("Received beacon:.*(10.11.12.\d+):.*,(\d+)\)")

HOST = {'10.11.12.11': 'lonestar',
        '10.11.12.24': 'maredsous',
        '10.11.12.36': 'bigfoot',
        '10.11.12.35': 'spaten',
        '10.11.12.17': 'shiner',
        '10.11.12.16': 'adams',
        '10.11.12.34': 'negramodelo',
        '10.11.12.25': 'wynkoop',
        '10.11.12.13': 'manny',
        '10.11.12.20': 'guiness'}

# Mission 33
#NUM_CONTEXT = {12: 100,
#               13: 0,
#               14: 200,
#               15: 400,
#               16: 800,
#               17: 1600,
#               18: 3200,}

# Mission 34
NUM_CONTEXT = {1:0,
               2:10,
               3:20,
               4:30,
               5:40,
               6:50,
               7:60,
               8:80,
               9:70,
               10:90,
               11:100,
               12:0,
               13:10,
               14:20,
               15:20,
               16:30,
               17:40,
               18:40,
               19:50,
               20:60,
               21:70,
               22:70,
               23:80,}

def main():
    experiments = {}
    
    results_dir = os.getcwd()
    for host in sorted(os.listdir(results_dir)):
        print "Processing %s's directory" % host
        
        for experimentFilename in sorted(os.listdir(host)):
            match = RE_EXPNUM.search(experimentFilename)
            if not match:
                continue
            
            expNum = int(match.group(1))
            
#            if not 12 <= expNum <= 18:
#                continue
#            
            print "Processing file: %s (Experiment %i)" % (experimentFilename, expNum)
            
            with open(os.path.join(results_dir, host, experimentFilename)) as expFile:
                received = {}
                for line in expFile:                        
                    match = RE_RECEIVED.search(line)
                    if match:
                        ip = match.group(1)
                        receivedId = int(match.group(2))
                        hostReceived = HOST[ip]
                        
                        receivedFromHost = received.get(hostReceived, [])
                        receivedFromHost.append(receivedId)
                        received[hostReceived] = receivedFromHost
                
                experimentEntry = experiments.get(expNum, {})
                experimentEntry[host] = received
                
                experiments[expNum] = experimentEntry
                
                
    experimentalResults = []
    for expNum in sorted(experiments.keys()):
        experimentEntry = experiments[expNum]
        
        print "Experiment %d Analysis" % expNum

        percentReceivedsBySender = {}
        for host in sorted(experimentEntry.keys()):
            hostEntry = experimentEntry[host]
            
            for receivedHost in sorted(experimentEntry.keys()):
                receivedIds = hostEntry.get(receivedHost, [])
                
                if len(receivedIds) < 2:
                    countSent = 1
                    countReceived = 0
                else:
                    first = receivedIds[0] 
                    last = receivedIds[-1]
                    
                    # don't count the first and the last but use them to know how many were sent
                    countSent = last - first - 1
                    countReceived = len(receivedIds) - 2 if len(receivedIds) >= 2 else 0

                percentReceived = float(countReceived) / countSent * 100 if countReceived > 0 else 0
                
                percentReceivedBySender = percentReceivedsBySender.get(receivedHost, [])
                percentReceivedBySender.append(percentReceived)
                
                percentReceivedsBySender[receivedHost] = percentReceivedBySender
                
                print "Host %s received %d (%.2f%%) from host %s" % (host, countReceived, percentReceived, receivedHost)
                
            print "-"*10
        
            
        averages = []
        for host in sorted(percentReceivedsBySender):
            percents = percentReceivedsBySender[host]
            average = sum(percents)/len(percents)
            print "%.2f%% of the packets from host %s were received" % (average, host)

            averages.append(average)
        
        print '-'*10
        
        averageOfAverages = sum(averages)/len(averages)
        print "%.2f%% of all the packets were received" % (averageOfAverages)
             
        experimentalResults.append((expNum, NUM_CONTEXT[expNum], averageOfAverages))
        
        print "*"*10
        
    pprint.pprint(experimentalResults)
                
                
                
                

            
if __name__ == '__main__':
    main()