#!/bin/bash

JVERS=`java -version 2>&1 | head -n 1 | cut -d "\"" -f 2 | cut -d "_" -f 1`
DEST_DIR="build/java_$JVERS"
#TODO compile lobby
#Pack Lobby Server
jar cfe lobby.jar org.bajnarola.lobby.LobbyServer org/bajnarola/lobby/LobbyServer.class \
		org/bajnarola/lobby/LobbyServer_Stub.class org/bajnarola/lobby/LobbyController.class \
		org/bajnarola/networking/NetPlayer.class org/bajnarola/utils/BajnarolaRegistry.class

#TODO compile game
#Pack game
jar cfm game.jar gameManifest.txt org/bajnarola/game/*.class org/bajnarola/game/*/*.class org/bajnarola/networking/*.class org/bajnarola/utils/*.class org/bajnarola/lobby/*.class lib/* -J-Djava.library.path=lib/native:lib/native -J-Dfile.encoding=UTF-8 
mkdir -p "$DEST_DIR"

mv lobby.jar "$DEST_DIR"
mv game.jar "$DEST_DIR"
ln -sf ../../lib "$DEST_DIR" 2> /dev/null
ln -sf ../../res "$DEST_DIR" 2> /dev/null