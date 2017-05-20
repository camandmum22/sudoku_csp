CC=javac
CFLAGS=-d build/ -cp src/
DEPS=sudoku_csp.class

MAIN: $(DEPS)

run:
	cd build; java Main

%.class: src/%.java
	$(CC) $(CFLAGS) $<

clean:
	rm build/*.class
