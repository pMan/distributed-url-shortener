#!/bin/bash
# stop all app instances

# SIG_INT is perfect to trigger the registered shutdown hook.
ARG='INT'

ps -ef | grep 'distributed-url-shortener.*\.jar' | grep -v grep | awk '{print $2}' | xargs kill -$ARG

