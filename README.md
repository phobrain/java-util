# java-util
Java library and utilities for photo analysis and preparation for Phobrain 

Target structure:

    build.gradle
    shared/
    shared/build.gradle
    shared/src/main/java/com/priot/util/...
    testpfr/
    testpfr/build.gradle
    testpfr/src/main/java/com/priot/testpfr/TestPairsReader.java


======== Notes for TestPairsReader.java

This program replicates on-disk, opened-readonly data corruption, seen using 3 JVM's on only 1 of 3 machines tested: multiple files being read synchronously a line at a time by multiple reader threads which queue their results.

    Read via: BufferedReader

    Queueing/synchronization via: LinkedBlockingQueue

Since I've only seen it on one of three machines, running two versions of the same OS (seen on i9 laptop on 2 diff m.2 drives, not on i7 desktop), I think it may be hardware-related, BUT HOW CAN A READONLY FILE BE CORRUPTED? And suspiciously when multiple files are open for read and the results are synchronously queued. OS is Pop!OS.

Seen on 3 JDK's. Oracle bug here:

https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8243422

=== Step 0: compile as you will, I can't get my gradle structure into github yet. See jbug.tgz for a gradle-compilable version.

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

Success:

    Ids from input_8000_ids_150_files/idlist: 8000
    avg dirs: [input_8000_ids_150_files]
    PairFilesReader: read threads: 5
    PairFileAggregator: 'testAvg' pos:  dirlist size 1
    Aggregator: add val12, val21:  dirs 1
    PairFilesReader: init with 150 files 5 reader threads  at Mon Apr 20 23:11:51 PDT 2020
    Ids: 8000 Pairs: 31996000
    done: 99%  expect: Mon Apr 20 23:17:07 PDT 2020                       PairFilesReader reader 4 internally done reading at line 31996001 Q size is 474
    PairFilesReader reader 2 internally done reading at line 31996001 Q size is 424
    PairFilesReader reader 1 internally done reading at line 31996001 Q size is 473
    PairFilesReader reader 0 internally done reading at line 31996001 Q size is 467
    PairFilesReader reader 3 internally done reading at line 31996001 Q size is 0
    done: 100%  expect: Mon Apr 20 23:17:07 PDT 2020                       PairFilesReader: check if done on pvQs:  0 1 2 3 4 done
    PairFilesReader.done(): read 190 GB in 5 min rate 617 MB/sec using 5 readers
    DONE
    
    real	5m16.916s
    user	28m0.535s
    sys	    2m18.415s
    
Failure:

    ...
    Ids: 8000 Pairs: 31996000
    done: 55%  expect: Mon Jul 06 17:59:28 PDT 2020                       Mon Jul 06 17:56:41 PDT 2020 - Error: PairFilesReader: reader 4: id0
    ~/input_8000_ids_150_files/143.pairs: File 147: mismatch: line 17600158
    expected: [2633:2633 6452:6452]
    Got:      [263322633 6452:6452]
    Actual line: [263322633 6452:6452 3.5200315E7 3.5200316E7]

    real	3m23.828s
    user	16m51.595s
    sys 	1m49.870s
    
The change of ':' -> '2' (octal 072->062) is a fairly-common pattern, but not the only one seen. Another example of the same flip with different neighboring bits, 

    expected: [3077:3077 6528:6528]
    Got:      [3077:3077 6520:6528]
    Actual line: [3077:3077 6520:6528 3.9767895E7 3.9767896E7]
    
'8' -> '0', octal 070->060, i.e. the 7->6 commonality is binary 111->110. I can see a bit getting corrupted on read, but how does it wind up back on disk? (Sometimes it doesn't, as well.)
    
The memory isn't ECC, but has passed extensive tests, repeated with the two dimms swapped.
