#!/bin/bash
ant && cd build && java ArmBuildDriver -d $*
