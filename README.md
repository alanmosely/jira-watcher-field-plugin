# Jira Watcher Field Plugin

## Overview

The Jira Watcher Field Plugin allows users to easily track and manage watchers on issues. It provides a customisable field that displays the list of watchers for any given issue, making it easier to keep stakeholders informed.

## Jira 11 Compatibility

Version 3.x targets Jira Data Center 11 (tested against the 11.3 LTS line). Jira 11 is built on
Platform 8 (JDK 21, Spring 6, Jakarta EE 10), so this version:

- builds with JDK 21 (`maven.compiler.release=21`)
- uses `jakarta.inject` annotations with Atlassian Spring Scanner 6.x (matching the runtime bundled in Jira 11)
- uses SLF4J for logging (the `org.apache.log4j` bridge is no longer relied upon)

For Jira 10, use the 2.10.x releases.

## Jira 10 Compatibility

I took the abandoned plugin code from <https://bitbucket.org/sxbehnke/jira-watcher-field-plugin/src/master/> and made it compatible with Jira 10
