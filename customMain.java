package cloudsim.grzegorz;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.power.Constants;
import org.cloudbus.cloudsim.network.datacenter.AggregateSwitch;
import org.cloudbus.cloudsim.network.datacenter.EdgeSwitch;
import org.cloudbus.cloudsim.network.datacenter.NetDatacenterBroker;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudlet;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenter;
import org.cloudbus.cloudsim.network.datacenter.NetworkHost;
import org.cloudbus.cloudsim.network.datacenter.NetworkVm;
import org.cloudbus.cloudsim.network.datacenter.NetworkVmAllocationPolicy;
import org.cloudbus.cloudsim.network.datacenter.RootSwitch;
import org.cloudbus.cloudsim.power.models.PowerModelSqrt;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


//customMain based on TestExample.java 
public class customMain {

	/** The vmlist. */
	private static List<NetworkVm> vmlist;
	
	protected static List<? extends NetworkCloudlet> cloudletList;

	/**
	 * Creates main() to run this example.
	 * 
	 * @param args
	 *            the args
	 */
	public static void main(String[] args) {

		Log.printLine("Starting CloudSimExample1...");

		try {
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; 

			CloudSim.init(num_user, calendar, trace_flag);

			
			NetworkDatacenter datacenter0 = createDatacenter("Datacenter_0");
			
			
			
			NetDatacenterBroker broker = createBroker("Broker");
			int brokerId = broker.getId();
			Log.printLine("BrokerId " + brokerId);
			broker.setLinkDC(datacenter0);

			vmlist = new ArrayList<NetworkVm>();
			cloudletList = new ArrayList<NetworkCloudlet>();


			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);
			
			CloudSim.startSimulation();

			CloudSim.stopSimulation();
			Log.printLine("DataCenter0 Custom");
			Map<String, Integer> m = new HashMap<>();
			for(Host host : datacenter0.getHostList()) {
				Log.printLine("Vm List for Host " + host.getId() + ": " + host.getVmList());
				VnetHost host1 = (VnetHost)host;
				if(!m.containsKey(host1.sw.getName())) {
					EdgeSwitch esw = (EdgeSwitch)host1.sw;
					m.put(host1.sw.getName(), esw.packetCount);
				}
				AggregateSwitch asw = (AggregateSwitch)host1.sw.uplinkswitches.get(0);
				if(asw != null) {
					if(!m.containsKey(asw.getName())) {
						
						m.put(asw.getName(), asw.packetCount);
					}
					RootSwitch rsw = (RootSwitch)host1.sw.uplinkswitches.get(0).uplinkswitches.get(0);
					if(!m.containsKey(rsw.getName())) {
						
						m.put(rsw.getName(), rsw.packetCount);
					}
				}
				
			}
			
			for(Map.Entry entry : m.entrySet()) {
				Log.printLine("Switch " + entry.getKey() + " has packets " + entry.getValue());
			}
			

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			System.out.println("numberofcloudlet " + newList.size() + " Cached "
					+ NetDatacenterBroker.cachedcloudlet + " Data transfered "
					+ NetworkConstants.totaldatatransfer);

			Log.printLine("CloudSimExample1 finished!");
			
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the datacenter.
	 * 
	 * @param name
	 *            the name
	 * 
	 * @return the datacenter
	 */
	private static NetworkDatacenter createDatacenter(String name) {

		List<VnetHost> hostList = new ArrayList<VnetHost>();

		int mips = 10;
		int ram = 6096; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 100000;
		for (int i = 0; i < NetworkConstants.EdgeSwitchPort * NetworkConstants.AggSwitchPort
				* NetworkConstants.RootSwitchPort; i++) {
			
			Log.printLine("Number of host: " + i);
			List<Pe> peList = new ArrayList<Pe>();
			peList.add(new Pe(0, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(1, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(2, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(3, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(4, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(5, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(6, new PeProvisionerSimple(mips))); 
			peList.add(new Pe(7, new PeProvisionerSimple(mips))); 
			
			hostList.add(new VnetHost(
					i,
					new RamProvisionerSimple(ram + (i*3)),
					new BwProvisionerSimple(bw + (i*3)),
					storage,
					peList,
					new VmSchedulerTimeShared(peList),
					new PowerModelSqrt(39.5, 0.2))); 
		}

		Log.printLine(hostList);

		String arch = "x86"; 
		String os = "Linux"; 
		String vmm = "Xen";
		double time_zone = 10.0; 
		double cost = 3.0; 
		double costPerMem = 0.05; 
		double costPerStorage = 0.001; 
		
		double costPerBw = 0.0; 
		LinkedList<Storage> storageList = new LinkedList<Storage>(); 

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch,
				os,
				vmm,
				hostList,
				time_zone,
				cost,
				costPerMem,
				costPerStorage,
				costPerBw);
		
		
		//Custom Allocation Policy - algorithm with virtual networks
		CustomVMAllocationPolicy customVMAllocationPolicy = new CustomVMAllocationPolicy(hostList);
		//Simple Allocation Policy - algorithm without virtual networks
		SimpleVmAllocationPolicy simpleVMAllocationPolicy = new SimpleVmAllocationPolicy(hostList);
		
		NetworkDatacenter datacenter = null;
		
		try {
			datacenter = new NetworkDatacenter(
					name,
					characteristics,
					simpleVMAllocationPolicy,
					//new NetworkVmAllocationPolicy(hostList), //change for different test scenarios
					storageList,
					50);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		CreateNetwork(2, datacenter);
		
		
		Map<String, Map<Integer, Integer>> networkMap = new HashMap<String, Map<Integer, Integer>> ();
		networkMap.put("HOST", datacenter.HostToSwitchid);
		networkMap.put("EDGE", datacenter.EdgeIdToAggId);
		networkMap.put("AGG", datacenter.AggIdtoRootId);
		
		customVMAllocationPolicy.networkMap = networkMap;
		
		
		
		return datacenter;
	}

	/**
	 * Creates the broker.
	 * 
	 * @return the datacenter broker
	 */
	private static NetDatacenterBroker createBroker(String name) {
		NetDatacenterBroker broker = null;
		try {
			broker = new NetDatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects.
	 * 
	 * @param list
	 *            list of Cloudlets
	 * @throws IOException
	 */
	private static void printCloudletList(List<Cloudlet> list) throws IOException {
		int size = list.size();
		Cloudlet cloudlet;
		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID"
				+ indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");
				Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent
						+ cloudlet.getVmId() + indent + indent + dft.format(cloudlet.getActualCPUTime())
						+ indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}

	}

	static void CreateNetwork(int numhost, NetworkDatacenter dc) {

		EdgeSwitch edgeswitch[] = new EdgeSwitch[(int) (NetworkConstants.RootSwitchPort * NetworkConstants.AggSwitchPort)];
		AggregateSwitch aggswitch[] = new AggregateSwitch[(int) NetworkConstants.RootSwitchPort];
		RootSwitch rootswitch[] = new RootSwitch[1];
		rootswitch[0] = new RootSwitch("Root0", NetworkConstants.ROOT_LEVEL, dc );
		Log.printLine("Creating switch Root0 id: " + rootswitch[0].getId());
		for (int j = 0; j < NetworkConstants.RootSwitchPort; j++) {
			aggswitch[j] = new AggregateSwitch("Agg" + j, NetworkConstants.Agg_LEVEL, dc );
			Log.printLine("Creating switch Agg" + j + " id: " + aggswitch[j].getId());
			for (int i = 0; i < (int) NetworkConstants.AggSwitchPort; i++) {
				int k = ((int) NetworkConstants.AggSwitchPort * j) + i;
				edgeswitch[k] = new EdgeSwitch("Edge" + k, NetworkConstants.EDGE_LEVEL, dc);
				Log.printLine("Creating switch Edge" + k + " id: " + edgeswitch[k].getId());
				edgeswitch[k].uplinkswitches.add(aggswitch[j]);
				dc.Switchlist.put(edgeswitch[k].getId(), edgeswitch[k]);
				dc.EdgeIdToAggId.put(edgeswitch[k].getId(), aggswitch[j].getId());
				aggswitch[j].downlinkswitches.add(edgeswitch[k]);
				
			}
			aggswitch[j].uplinkswitches.add(rootswitch[0]);
			rootswitch[0].downlinkswitches.add(aggswitch[j]);
			dc.Switchlist.put(aggswitch[j].getId(), aggswitch[j]);
			dc.AggIdtoRootId.put(aggswitch[j].getId(), rootswitch[0].getId());
		}
		dc.Switchlist.put(rootswitch[0].getId(), rootswitch[0]);
		

		for (Host hs : dc.getHostList()) {
			VnetHost hs1 = (VnetHost) hs;
			hs1.bandwidth = NetworkConstants.BandWidthEdgeHost;
			int switchnum = (int) (hs.getId() / NetworkConstants.EdgeSwitchPort);
			edgeswitch[switchnum].hostlist.put(hs.getId(), hs1);
			dc.HostToSwitchid.put(hs.getId(), edgeswitch[switchnum].getId());
			hs1.sw = edgeswitch[switchnum];
			List<VnetHost> hslist = hs1.sw.fintimelistHost.get(0D);
			if (hslist == null) {
				hslist = new ArrayList<VnetHost>();
				hs1.sw.fintimelistHost.put(0D, hslist);
			}
			hslist.add(hs1);


		}
		
		//Print Switch assignment
		Log.printLine("---HOST to SWITCH");
		Log.printLine(dc.HostToSwitchid.toString());
		Log.printLine("---EDGE to AGG");
		Log.printLine(dc.EdgeIdToAggId.toString());
		Log.printLine("---AGG to ROOT");
		Log.printLine(dc.AggIdtoRootId.toString());
		
		

	}
}
