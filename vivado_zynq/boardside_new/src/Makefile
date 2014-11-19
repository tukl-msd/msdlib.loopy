CC=gcc
CFLAGS=-c -Wall
LDFLAGS=
SOURCES=main.c io.c queueUntyped.c scheduler.c components/components.c medium/medium.c medium/message.c medium/protocol/protocol_v1.c medium/protocol/protocol_v2.c
OBJECTS=$(SOURCES:.c=.o)
EXECUTABLE=board-side

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.c.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm -rf $(OBJECTS)
	rm -rf $(EXECUTABLE)
