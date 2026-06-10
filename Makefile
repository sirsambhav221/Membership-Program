JAVA ?= java
JAVAC ?= javac
PORT ?= 8080

JAVA_SOURCES := $(shell find src/main/java -name "*.java")

.PHONY: compile run demo clean

compile:
	mkdir -p target/classes
	$(JAVAC) -d target/classes $(JAVA_SOURCES)

run: compile
	$(JAVA) -cp target/classes com.firstclub.membership.Application $(PORT)

demo: compile
	$(JAVA) -cp target/classes com.firstclub.membership.DemoRunner

clean:
	rm -rf target
