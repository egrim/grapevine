#! /usr/bin/python
import os
import re
import pprint

RE_EXPNUM = re.compile("Exp(\d+)-")
RE_SENT = re.compile("Broadcasting Beacon:.*,(\d+)\)")
RE_RECEIVED = re.compile("Received beacon:.*(10.11.12.\d+):.*,(\d+)\)")

HOST = {'10.11.12.11': 'lonestar',
        '10.11.12.24': 'maredsous',
        '10.11.12.36': 'bigfoot',
        '10.11.12.35': 'spaten',
        '10.11.12.17': 'shiner',
        '10.11.12.16': 'adams',
        '10.11.12.34': 'negramodelo',
        '10.11.12.25': 'wynkoop',
        '10.11.12.13': 'manny'}

class Host:
    sent = {}
    received = {}

def main():
    experiments = {}
    
    results_dir = os.getcwd()
    for host in sorted(os.listdir(results_dir)):
        print "Processing %s's directory" % host
        
        for experimentFilename in sorted(os.listdir(host)):
            expNum = int(RE_EXPNUM.search(experimentFilename).group(1))
            
            print "Processing file: %s (Experiment %i)" % (experimentFilename, expNum)
            
            experimentHostEntry = {}
            with open(os.path.join(results_dir, host, experimentFilename)) as expFile:
                sentSet = set()
                received = {}
                for line in expFile:
                    match = RE_SENT.search(line)
                    if match:
                        sentId = int(match.group(1))
                        sentSet.add(sentId)
                        
                    match = RE_RECEIVED.search(line)
                    if match:
                        ip = match.group(1)
                        receivedId = int(match.group(2))
                        hostReceived = HOST[ip]
                        receivedSet = received.get(hostReceived, set())
                        receivedSet.add(receivedId)
                        received[hostReceived] = receivedSet
                
                experimentHostEntry['sent'] = sentSet
                experimentHostEntry['received'] = received

                experimentEntry = experiments.get(expNum, {})
                experimentEntry[host] = experimentHostEntry
                
                experiments[expNum] = experimentEntry
                
                
                
    pprint.pprint(experiments)                
                        

            
if __name__ == '__main__':
    main()