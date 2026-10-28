# ==============================================================================
# NanoFiles P2P — Build & Execution Makefile
# ==============================================================================

JAVAC      := javac
JAR        := jar
JAVA       := java
JFLAGS     := -encoding UTF-8 -Xlint:none

SRC_DIR    := src/main/java
BUILD_DIR  := target/classes
DIST_DIR   := target

SOURCES    := $(shell find $(SRC_DIR) -name "*.java")

MAIN_PEER  := es.um.redes.nanoFiles.application.NanoFiles
MAIN_DIR   := es.um.redes.nanoFiles.application.Directory

PEER_JAR   := $(DIST_DIR)/NanoFiles.jar
DIR_JAR    := $(DIST_DIR)/Directory.jar

.PHONY: all compile jar run-dir run-peer clean help

all: jar

compile:
	@mkdir -p $(BUILD_DIR)
	$(JAVAC) $(JFLAGS) -d $(BUILD_DIR) $(SOURCES)

jar: compile
	@mkdir -p $(DIST_DIR)
	$(JAR) cfe $(PEER_JAR) $(MAIN_PEER) -C $(BUILD_DIR) .
	$(JAR) cfe $(DIR_JAR) $(MAIN_DIR) -C $(BUILD_DIR) .
	@echo "Built $(PEER_JAR) and $(DIR_JAR)"

run-dir: jar
	$(JAVA) -jar $(DIR_JAR)

run-peer: jar
	$(JAVA) -jar $(PEER_JAR) nf-shared

clean:
	rm -rf target bin

help:
	@echo "NanoFiles P2P Build Targets:"
	@echo "  make          - Compile sources and build Directory.jar & NanoFiles.jar"
	@echo "  make compile  - Compile Java sources into target/classes"
	@echo "  make jar      - Package executable JAR archives into target/"
	@echo "  make run-dir  - Start the central UDP Directory Server (port 6868)"
	@echo "  make run-peer - Start an interactive NanoFiles peer node"
	@echo "  make clean    - Remove build artifacts"
