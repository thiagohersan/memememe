# run memememeSelfie python
isrun=`ps -u root | grep python | wc -l`
if [ $isrun -lt 1 ]
then
    cd /home/pi/Dev/memememe/Python/selfieMemememe
    cnt=0
    while [ $cnt -lt 50 ]
    do
	sudo python selfieMemememe.py &
	sleep 1
	killsudopid=$!
	killpythonpid=`ps -u root | awk '/python/{print $1}'`
	sleep 600
	sudo kill -9 $killpythonpid
	sudo kill -9 $killsudopid
	let cnt=cnt+1
	echo $cnt
    done
    sudo halt -n
fi
