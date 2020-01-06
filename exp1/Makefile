compile: clean
	mkdir build
	/home/mashplant/Code/IdeaProjects/compiler_train/jdk_installer/jdk1.5.0_22/bin/javac -Xlint:-options -source 1.5 -target 1.5 \
	  -cp lib/joeq.jar \
	  -sourcepath src -d build `find src -name "*.java"`

clean:
	rm -rf build/
