#!/bin/bash
ant && cd build && java ArmPlatformDriver -d $*
