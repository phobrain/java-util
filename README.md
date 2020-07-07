# java-util
Java library and utilities for photo analysis and preparation for Phobrain 

======== Notes for TestPairsReader.java

This program replicates on-disk, opened-readonly data corruption, with multiple files being read synchronously a line at once by multiple reader threads which queue their results.

    Read via: BufferedReader

    Queueing/synchronization via: LinkedBlockingQueue

Since I've only seen it on one of three machines, running two versions of the same OS (seen on i9 laptop on 2 diff m.2 drives, not on i7 desktop), I think it may be hardware-related, BUT HOW CAN A READONLY FILE BE CORRUPTED? And suspiciously when multiple files are open for read and the results are synchronously queued. OS is Pop!OS.

Seen on 3 JDK's. Oracle bug here:

https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8243422

=== Step 0: compile as you will, I can't get my gradle structure into github yet.

=== Step 1: gen input data: 8K id's (N*(N-1))/2 lines of pairs across 150 file

$ java <...> gen 8000 150
Writing 31996000 lines to 150 files
done: 100%  expect: Mon Apr 20 20:23:03 PDT 2020                       
Printed 150 files w/ 8000 ids
        in input_8000_ids_150_files/ id list: input_8000_ids_150_files/idlist

real    88m45.102s
user    27m48.520s
sys     60m21.579s

=== Step 2: run test

$ java <...> ./input_8000_ids_150_files
