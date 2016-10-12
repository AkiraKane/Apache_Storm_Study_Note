TopologyBuilder builder = new TopologyBuilder();

builder.setSpout("1", new TestWordSpout(true), 5);
builder.setSpout("2", new TestwordSpout(true), 3);
builder.setBolt("3", TestWordCounter(), 3)
			.fieldsGrouping("1", new Fileds("word"))
			.fieldsGrouping("2", new Fields("word"));
builder.setBolt("4", new TestGlobalCount())
			.globalGrouping("1");

Map conf = new HashMap();
conf.put(Config.TOPOLOGY_WORKERS, 4);
conf.put(Config.TOPOLOGY_DEBUG, true);

LocalCluster cluster = new LocalCluster();
cluster.submitTopology("mytopology", conf, createTopology());
Utils.sleep(10000);
cluster.shutdown();

// createTopology
public StormTopology createTopology()

// setBolt
public BoltDeclarer setBolt(String id, 
							IRichBolt bolt,
							Number parallelism_hint)
					throws IllegalArgumentException

// When writing topologies using Java, IRichBolt and IRichSpout are the main interfaces to use to implement components of the topology.
// id - the id of this component. This id is referenced by other component that want to consume this bolt's outputs.
// bolt - the bolt
// parallelism_hint - the number of tasks that should be assigned to execute this bolt. Each task will run on a thread in a process somewhere around the cluster. 
// use the returned object to declare the inputs to this component.

// setSpout
public SpoutDeclarer setSpout(String id,
							  IRichSpout spout,
							  Number parallelism_hint)
					throws IllegalArgumentException

// Define a new spout in this topology with the specified parallelism. If the spout declares itself as non-distributed, the parallelism_hint will be ignored and only one task will be allocated to this component. 

// id - the id of this component. This id is referenced by other components that want to consume this spout's outputs. 
// spout - the spout
// parallelism_hint - the number of tasks that should be assigned to execute this spout. Each task will run on a thread in a process somewhere around the cluster. 

// Local Mode
import org.apache.storm.LocalCluster; 
LocalCluster cluster = new LocalCluster(); 
// You can then submit topologies using the submitTopology method on the LocalCluster object. Just like the corresponding method on StormSubmitter, submitTopology takes a name, a topology configuration, and the topology object. You can then kill a topology using the killTopology method which takes the topology name as an argument.

// to shutdown a local cluster, simple call:
cluster.shutdown();

// What does it mean for a message to be "fully processed"
// A tuple coming off a spout can trigger thousands of tuples to be created based on it. Consider, for example, the streaming word count topology

TopologyBuilder builder = new TopologyBuilder();
builder.setSpout("sentences", new KestrelSpout("kestrel.backtype.com", 22133, "sentence-queue", new StringSchema()));
builder.setBolt("split", new SplitStence(), 10)
		.shuffleGrouping("sentence"); 

builder.setBolt("count", new WordCount(), 20)
		.fieldsGrouping("split", new Fielfds("word"));

// Here is the interface that spouts implement
public interface ISpout extends Serializable{
	void open(Map conf, TopologyContext context, SpoutOutputCollector collector); 
	void close(); 
	void nextTuple(); 
	void ack(Object msgId); 
	void fail(Object msgId);
}

// First, Storm requests a tuple from the Spout by calling the nextTuple method on the Spout. The Spout uses the SpoutOutputCollector provided in the open method to emit a tuple to one of its output streams. When emitting a tuple, the Spout provides a "message id" that will be used to identify the tuple later. For example, KestrelSpout reads a message off of the kestrel queue and emits as the "message id" the id provided by kestrel for the message. Emitting a message to the SpoutOutputCollector looks like this:  

_collector.emit(new Values("field1", "field2", 3), msgId); 

// Next, the tuple gets sent to the consuming bolts and Storm takes care of tracking the tree of message that is created. If Storm detects that a tuple is fully processed, Storm will call the ack method on the originating Spout task with the message id that the Spout provided to the Storm. Likewise, if the tuple times-out, Storm will call the fail method on the Spout. Note that a tuple will be acked or failed by the exact same Spout task that created it. So if a Spout is executing as many as tasks across the cluster, a tuple won't be acked or failed by different task than the one that created it. 

// What is Storm's reliability API? 
// There are two things you have to do as a user to benefit from Storm's reliability capabilities. First, you need to tell Storm whenever you're creating a new link in the tree of tuples. Second, you need to tell Storm when you have finished processing an individual tuple. By doing both these things, Storm can detect when the tree of tuples is fully processed and can ack or fail the spout tuple appropriately. Storm's API provides a concise way of doing both of these tasks.
// Specifying a link in the tuple tree is called anchoring. Anchoring is done at the same time you emit a new tuple. Let's use the following bolt as an example. This bolt splits a tuple containing a sentence into a tuple for each word.

public class SplitSentence exntends BaseRichBolt{
	OutputCollector _collector;

	public void prepare(Map conf, TopologyContext context, OutputCollector collector){
		_collector = collector;
	}

	public void execute(Tuple tuple){
		String sentence = tuple.getString(0);
		for(String word: sentence.split(" ")){
			_collector.emit(tuple, new Values(word)); 
		}
		_collector.ack(tuple);
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer){
		declarer.declare(new Fields("word"));
	}
}

// Data Model
// This bolt declares that it emits 2-tuples with fields "double" and "triple":

public class DoubleAndTripleBolt extends BasicRichBolt{
	private OutputCollectorBase _collector;

	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollectorBase collector){
		_collector = collector;
	}

	@Override
	public void execute(Tuple input){
		int val = input.getInteger(0);
		_collector.emit(input, new Values(val*2, val*3));
		_collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer){
		declarer.declare(new Fields("double", "triple"));
	}
}

// A Simple Topology
TopologyBuilder builder = new TopologyBuilder();
builder.setSpout("words", new TestWordSpout(), 10);
builder.setBolt("exclaim1", new ExclamationBolt(), 3)
			.shuffleGrouping("words");
builder.setBolt("exclaim2", new ExclamationBolt(), 2)
			.shuffleGrouping("exclaim1");

// If you wanted component "exclaim2" to read all the tuples emitted by both component "words" and component "exclaim1", you would write component "exclaim2" like this:
builder.setBolt("exclaim2", new ExclamationBolt(), 5)
			.shuffleGrouping("words")
			.shuffleGrouping("exclaim1"); 

public void nextTuple(){
	Utils.sleep(100);
	final String[] words = new String[]{"nathan","mike","jackson","golda","bertels"};
	final Random rand = new Random();
	final String word = words[rand.nextInt(words.length)];
	_collector.emit(new Values(word));
}

// ExclamationBolt appends the string "!!!" to its list
public static class ExclamationBolt implements IRichBolt{
	OutputCollector _collector; 

	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector){
		_collector = collector;

	}

	@Override
	public void execute(Tuple tuple){
		_collector.emit(tuple, new Values(tuple.getString(0) + "!!!"));
		_collector.ack(tuple);
	}

	@Override
	public void cleanup(){}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer){
		declarer.declare(new Fields("word"));
	}

	@Override
	public Map<String,Object> getComponentCongfiguration(){
		return null;
	}
}