=== Generating testdata ===

1. Create a folder where the test instances should be stored.
2. Copy the files generate_all.py and testdata.py from /test/python into this folder.
3. Make sure Python27 AND! Python3x are installed.
4. Check (and modify) the path for Python27 in the file generate_all.py
5. Alter "instancesPerCommit" at the top of generate_all.py to specified the sizes of the testdata to be generated.
   1000 means 1000 inserts/updates/deletes per commit.
6. Run generate_all.py using Python3x
7. Wait for the script to fully complete executing. Multiple folders for the testdata are created.
   When running the script again these folder are deleted an recreated.

A (highly) compressed version of the resulting testdata is also available in the test directory.
(containing both scenarios in sizes 100, 1000, 10000, 100000, 1000000)



=== Installation of OrientDB ===

There are two options available
 - embedded mode (recommended)
 - remote mode

Embedded mode can be used out of the box, maven depencies are configured.
Use "EMBEDDED_MODE = true" setting in test/benchmarking/Benchmarking.java to use embedded mode. (default)
Typically faster than remote mode.

For remote mode a standalone OrientDB installation is needed. The current version can be downloaded at
http://www.orientechnologies.com/download/ (orientdb-community-2.0.X). After extraction, start the server by the
shells script in /bin - a root account is created (provide password). Stop the database and add a user admin/admin
at the file config/orientdb-server-config.xml similar to the existing root users.
Use "EMBEDDED_MODE = false" setting in test/benchmarking/Benchmarking.java to use remote mode.
Start the OrientDB installation by the shell scripts in /bin before running benchmarks.
Remote mode is most useful for easy testing of queries in a graphical environment by default at http://localhost:2480/.
When using a remote mode, the versions of the installation and the versions in the maven dependencies should match.



=== Configuration of the Benchmarks ===

See test/benchmarking/BenchmarkingRunner.java.

Path for database location, test instances or results are specified there. Make sure all of this folder
exist before running the benchmarks. When using remote mode the database directory can be set to "".
The directory of the installation always used when in remote mode.

All the sizes specified in AVAILABLE_SCENARIO_SIZES are used in the testcase runAll.



=== Running the Benchmarks ===

After the steps above do:

1. open a terminal windows
2. change to directory openengsb\components\ekb\persistence-orientdb
3. run: mvn clean install
4. run: mvn test -Dtest=BenchmarkingRunner
   or   mvn test -Dtest=BenchmarkingRunner -Dstorage.useWAL=false       (not using write ahead log)

When aborting the run of the benchmark make sure that the process is correctly terminated.

Do not increase heap memory size (specified in maven surefire plugin, pom.xml) all the way up - OrientDB uses the
remaining ram as disk cache. However about 6GB are required for size 1000000 benchmarks
(4GB lead to a out of memory error).
