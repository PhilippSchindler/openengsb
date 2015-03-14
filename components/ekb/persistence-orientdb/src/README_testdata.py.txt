REQUIREMENTS

the following commands must be available on PATH:
- python (v2.7)
- java (v1.6)
- javac (v1.6)
- mvn (v2.x)
- git (v1.8.3)
- pip (python package installer)
- make sure JAVA_HOME is correctly set to the base dir of a JDK (not JRE)
- make sure that a git username/email address are defined in gitconfig. to
  test this, issue some git commands from CLI. no prompt for a username
  should be shown. if these parameters are missing, git commands during
  a testrun will halt and wait for user input.

install required python-eggs using pip. to do a complete testrun the following
packages must be installed:
- Flask v0.10.1: sudo pip install Flask
- requests v1.2.3: sudo pip install requests
- GitPython v0.3.2RC1: sudo pip install GitPython
- rdflib v4.0.1: sudo pip install rdflib
check that pip installs an appropriate version of the packages. pip on windows
seems to install older versions under unknown circumstances.



USAGE

mkdir scenario_1
mkdir scneario_2

testdata.py 1 -c -f 10 turtle scenario_1 
testdata.py 2 -c -f 10 turtle scenario_2 

1.. process for evaluation scenario 1
2.. process for evaluation scenario 2
10 is the factor, base 100 entries
supported formats: turtle, csv
scenario_1: output folder