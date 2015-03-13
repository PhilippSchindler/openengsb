import os
import shutil
import subprocess
import csv


instancesPerCommit = [ 100, 1000, 10000, 100000, 100000, 1000000 ]

ridPrefix = ""


def getRidDict(size, scenario):
    rid = 0
    d = dict()
    i = 0
    while True:
        filepath = "%i\\scenario_%i\scenario_%i_commit_%i_inserts.csv" % (size, scenario, scenario, i)
        if not os.path.isfile(filepath): break
        with open(filepath, newline='') as csvfile:
            reader = csv.reader(csvfile, delimiter=';')

            first = True
            for row in reader:
                if first: first = False
                else:     
                    d[row[0]] = rid
                    rid += 1
        i += 1
    return d


def replaceInstanceIdWithRid(size, scenario, ridDict):
    directory = "%i\\scenario_%i"  % (size, scenario)
    for filename in os.listdir(directory):
        filepath = os.path.join(directory, filename)
        
        with open(filepath, newline='') as inputFile:
            with open("temp.csv", "wt", newline='') as tempFile:
                                
                reader = csv.reader(inputFile, delimiter=';')
                writer = csv.writer(tempFile, delimiter=';')

                first = True
                for row in reader:
                    if first: first = False
                    else:     row[0] = ridPrefix + str(ridDict[row[0]])
                    writer.writerow(row)

        shutil.move("temp.csv", filepath) 
            



# delete existing stuff
for subdir in next(os.walk("."))[1]:
    shutil.rmtree(subdir)

print("cleaned old stuff")


# create dirs
for size in instancesPerCommit:
    path = str(size)
    os.mkdir(path)
    os.mkdir(os.path.join(path, "scenario_1"))
    os.mkdir(os.path.join(path, "scenario_2"))

print("directories created")


# generate csv files
for size in instancesPerCommit:
    subprocess.call('''"C:\\Python27\\python.exe" testdata.py 1 -c -f %i csv %i\\scenario_1''' % (size//100, size) )
    print("testdata for scenario 1 for size", size, "genertated")
    subprocess.call('''"C:\\Python27\\python.exe" testdata.py 2 -c -f %i csv %i\\scenario_2''' % (size//100, size) )
    print("testdata for scenario 2 for size", size, "genertated")


# transform instanceId to RID
for size in instancesPerCommit:
    for scenario in [1, 2]:
        d = getRidDict(size, scenario)
        replaceInstanceIdWithRid(size, scenario, d)
        print("instanceIds for scenario", scenario, "for size", size, "transformed to RIDs")




        





