#A simple distributed compute engine
#Experiments on remote method invocation, 
inspired by Java Tutorial of RMI
 
1. There're three roles: client who submits the task to a server node; distributor(server node) who distributes the incoming tasks to server nodes of next level, with some scheduling/distribution strategy; executor(server node) who executes the incoming task. One JVM runs only one distributor simutanously. One executor executes only one task simutanously.
2. Multi level scheduling is supported
           DistributorX
             /    \   \
    DistributorY   \   \
         /          \   \
     ExecutorC       \  ExecutorB
                  ExecutorA
3. "Job FIFO" is the current strategy
4. Changable scheduling strategy
5. Thread-safe design
6. No busy waiting
7. Generic: both the running process and required datatype can be general, these class file should be downloaded automatically by Java RMI framework. Tasks should implement Job abstract class.
8. Distributed: tasks are executed remotely, with many distributors, executors, and clients.

I'm not going to maintain this experiment code any more.
2016.3.20
