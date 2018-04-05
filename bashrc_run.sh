# run memememeSelfie python
isrun=`ps -u root | grep python | wc -l`
if [ $isrun -lt 1 ]
then
    cd /home/pi/Dev/memememe/Python/selfieMemememe
    cnt=0
    if [[ $(date +%u) -eq 4 ]] ; then maxcnt=132; else maxcnt=84; fi
    while [ $cnt -lt $maxcnt ]
    do
	sudo python selfieMemememe.py &
	sleep 1
	killsudopid=$!
	killpythonpid=`ps -u root | awk '/python/{print $1}'`
	sleep 300
	sudo kill -9 $killpythonpid
	sudo kill -9 $killsudopid
	let cnt=cnt+1
	echo $cnt
    done
    sudo halt -n
fi
