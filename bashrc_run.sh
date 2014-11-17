# run memememeSelfie python
isrun=`ps -u root | grep python | wc -l`
if [ $isrun -lt 1 ]
then
    cd /home/pi/Dev/memememe/Python/selfieMemememe
    rm -rf ./stop.sh
    while [ ! -f ./stop.sh ]
    do
	sudo python selfieMemememe.py &
	sleep 1
	killsudopid=$!
	killpythonpid=`ps -u root | awk '/python/{print $1}'`
	sleep 5
	sudo kill -9 $killpythonpid
	sudo kill -9 $killsudopid
    done
    rm -rf ./stop.sh
    #sudo halt -n
fi
