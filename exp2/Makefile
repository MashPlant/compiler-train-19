compile: clean
	mkdir build
	${JAVA_HOME}/bin/javac -Xlint:-options -source 1.5 -target 1.5 -Xlint:deprecation \
	  -cp lib/joeq.jar \
	  -sourcepath src -d build `find src -name "*.java"`

clean:
	rm -rf build/
