/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package cloudsim.grzegorz;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudletSpaceSharedScheduler;
import org.cloudbus.cloudsim.network.datacenter.NetworkHost;
import org.cloudbus.cloudsim.network.datacenter.HostPacket;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

/**
 * CustomVMAllocationPolicy 
 * 
 * Based on NetworkVmAllocationPolicy by 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 1.0
 */
public class CustomVMAllocationPolicy extends VmAllocationPolicy {

	/** The vm table. */
	private Map<String, Host> vmTable;
	
	private final List<Map<String, Object>> savedAllocation = new ArrayList<Map<String, Object>>();

	private VnetVmSelectionPolicy vmSelectionPolicy;
	
	private Map<String, List<Integer>> vnetHostTable; 

	/** The used pes. */
	private Map<String, Integer> usedPes;

	/** The free pes. */
	private List<Integer> freePes;
	
	private List<Double> usedMIPS;
	
	public Map<String, Map<Integer, Integer>> networkMap;

	/**
	 * @param list the list
	 * 
	 * @pre $none
	 * @post $none
	 */
	public CustomVMAllocationPolicy(List<? extends Host> list) {
		super(list);
		this.usedMIPS = new ArrayList<Double>();
		setFreePes(new ArrayList<Integer>());
		for (Host host : getHostList()) {
			double mipsTemp = 0;
			for (Vm vm2 : host.getVmList()) {
				mipsTemp += host.getTotalAllocatedMipsForVm(vm2);
			}
			usedMIPS.add(mipsTemp);
			getFreePes().add(host.getNumberOfPes());
		}
		
		vmSelectionPolicy = new VnetVmSelectionPolicy();

		setVmTable(new HashMap<String, Host>());
		setUsedPes(new HashMap<String, Integer>());
		setVnetHostTable(new HashMap<String, List<Integer>>());
		networkMap = null;
	}

	/**
	 * Allocates a host for a given VM.
	 * 
	 * @param vm VM specification
	 * 
	 * @return $true if the host could be allocated; $false otherwise
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {

		VnetVm vnetVm = (VnetVm) vm;
		int requiredPes = vm.getNumberOfPes();
		String requiredVnet = vnetVm.getVnet();
		Log.printLine("Vnet required: " + requiredVnet);
		VnetHost hostChoosen = null;
		boolean result = false;
		int tries = 0;
		
		List<Integer> freePesTmp = new ArrayList<Integer>();
		for (Integer freePes : getFreePes()) {
			freePesTmp.add(freePes);
		}

		if (!getVmTable().containsKey(vm.getUid())) { 
			if(requiredVnet != null && getVnetHostTable().containsKey(requiredVnet)) {
				
				//get list of hosts with vnet
				List<Integer> vnetSpecList = getVnetHostTable().get(requiredVnet);
				
				Log.printLine("Hosts with the same vnet: " + vnetSpecList);
				
				hostChoosen = findLowestPoweredHost(vnetSpecList, vnetVm);
				
				
				if(hostChoosen != null) {
					result = hostChoosen.vmCreate(vm);
					if(result) {
						getVmTable().put(vm.getUid(), hostChoosen);
						getUsedPes().put(vm.getUid(), requiredPes);
						if(getVnetHostTable().containsKey(requiredVnet)) {
							getVnetHostTable().get(requiredVnet).add(hostChoosen.getId()); 
						}else {
							ArrayList<Integer> tempHosts = new ArrayList<Integer>();
							tempHosts.add(hostChoosen.getId());
							getVnetHostTable().put(requiredVnet, tempHosts);
						}
						usedMIPS.set(hostChoosen.getId(), usedMIPS.get(hostChoosen.getId()) + hostChoosen.getTotalAllocatedMipsForVm(vm));
						Log.printLine("Added allocated MIPS for " + hostChoosen.getId());
						result = true;
						return result;
					}
				}
				
				if(!result) {
					int vnetHostId = vnetSpecList.get(0);
					int edgeId = this.networkMap.get("HOST").get(vnetHostId);
					List<Integer> tempEdgeHosts = new ArrayList<Integer> (); 
					for(Map.Entry<Integer, Integer> entry : this.networkMap.get("HOST").entrySet()) {
						if(entry.getValue() == edgeId) {
							tempEdgeHosts.add(entry.getKey());
						}
					}
					
					hostChoosen = findLowestPoweredHost(tempEdgeHosts, vnetVm);
					
					
					if(hostChoosen != null) {
						result = hostChoosen.vmCreate(vm);
						if(result) {
							getVmTable().put(vm.getUid(), hostChoosen);
							getUsedPes().put(vm.getUid(), requiredPes);
							if(getVnetHostTable().containsKey(requiredVnet)) {
								getVnetHostTable().get(requiredVnet).add(hostChoosen.getId()); 
							}else {
								ArrayList<Integer> tempHosts = new ArrayList<Integer>();
								tempHosts.add(hostChoosen.getId());
								getVnetHostTable().put(requiredVnet, tempHosts);
							}
							usedMIPS.set(hostChoosen.getId(), usedMIPS.get(hostChoosen.getId()) + hostChoosen.getTotalAllocatedMipsForVm(vm));
							Log.printLine("Added allocated MIPS for " + hostChoosen.getId());
							result = true;
							return result;
						}
					}
					
					if(!result) {
						int aggId = this.networkMap.get("AGG").get(edgeId);
						List<Integer> tempAggHosts = new ArrayList<Integer>();
						for(Map.Entry<Integer, Integer> entry : this.networkMap.get("EDGE").entrySet()) {
							if(entry.getValue() == aggId && entry.getKey() != edgeId) {
								for(Map.Entry<Integer, Integer> entry2 : this.networkMap.get("HOST").entrySet()) {
									if(entry2.getValue() == entry.getKey()) {
										tempAggHosts.add(entry2.getKey());
									}
								}
							}
							
						}
						
						hostChoosen = findLowestPoweredHost(tempAggHosts, vnetVm);
						
						
						if(hostChoosen != null) {
							result = hostChoosen.vmCreate(vm);
							if(result) {
								getVmTable().put(vm.getUid(), hostChoosen);
								getUsedPes().put(vm.getUid(), requiredPes);
								if(getVnetHostTable().containsKey(requiredVnet)) {
									getVnetHostTable().get(requiredVnet).add(hostChoosen.getId()); 
								}else {
									ArrayList<Integer> tempHosts = new ArrayList<Integer>();
									tempHosts.add(hostChoosen.getId());
									getVnetHostTable().put(requiredVnet, tempHosts);
								}
								usedMIPS.set(hostChoosen.getId(), usedMIPS.get(hostChoosen.getId()) + hostChoosen.getTotalAllocatedMipsForVm(vm));
								Log.printLine("Added allocated MIPS for " + hostChoosen.getId());
								result = true;
								return result;
							}
						}
						
						
					}
					
					
				}
				
			}
			
			if(!result) {
				List<Integer> tempHostList = new ArrayList<Integer>();
				for(Host host: getHostList()) {
					tempHostList.add(host.getId());
				}
				
				hostChoosen = findLowestPoweredHost(tempHostList, vnetVm);
				result = hostChoosen.vmCreate(vm);
				
				if(result) {
					getVmTable().put(vm.getUid(), hostChoosen);
					getUsedPes().put(vm.getUid(), requiredPes);
					if(requiredVnet != null) {
						if(getVnetHostTable().containsKey(requiredVnet)) {
							getVnetHostTable().get(requiredVnet).add(hostChoosen.getId()); 
						}else {
							ArrayList<Integer> tempHosts = new ArrayList<Integer>();
							tempHosts.add(hostChoosen.getId());
							getVnetHostTable().put(requiredVnet, tempHosts);
						}
					}
					usedMIPS.set(hostChoosen.getId(), usedMIPS.get(hostChoosen.getId()) + hostChoosen.getTotalAllocatedMipsForVm(vm));
					Log.printLine("Added allocated MIPS for " + hostChoosen.getId());
					//getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
					result = true;
				}
				
			}
			

		}

		return result;
	}
	
	private VnetHost findLowestPoweredHost(List<Integer> vnetHostList, Vm vm) {
		int moreFree = Integer.MIN_VALUE;
		double lessPower = Double.MAX_VALUE;
		VnetHost hostChoosen = null;
		for(Integer tempHostId :vnetHostList) {
			
			Log.printLine("Host: " + tempHostId);
			VnetHost host = (VnetHost) getHostList().get(tempHostId);
			double maxPower = getMaxPowerOfHost(host);
			double usedPower = getPowerOfHost(host);
			double powerAfterAlloc = getPowerAfterAllocation(host, tempHostId, vm);
			if(powerAfterAlloc != -1) {
				Log.printLine("maxPower: " + maxPower);
				Log.printLine("powerAfterAlloc: " + powerAfterAlloc);
				if(powerAfterAlloc <= maxPower) {
					if(powerAfterAlloc < lessPower) {
						lessPower = powerAfterAlloc;
						hostChoosen = host;
					}
				}
			}
			
		}
		return hostChoosen;
		
	}
	
	protected double getPowerAfterAllocation(VnetHost host, int hostId, Vm vm) {
		double power = 0;
		try {
			if(getMaxUtilizationAfterAllocation(host, hostId,  vm) >= 1) {
				return -1;
			}else {
				power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, hostId,  vm));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return power;
	}
	
	protected double getPowerOfHost(VnetHost host) {
		return host.getPower();
	}
	
	protected double getMaxPowerOfHost(VnetHost host) {
		return host.getMaxPower();
	} 

	/**
	 * Gets the power after allocation. We assume that load is balanced between PEs. The only
	 * restriction is: VM's max MIPS < PE's MIPS
	 * 
	 * @param host the host
	 * @param vm the vm
	 * 
	 * @return the power after allocation
	 */
	protected double getMaxUtilizationAfterAllocation(VnetHost host, int hostId,  Vm vm) {
		double requestedTotalMips = vm.getCurrentRequestedTotalMips();
		double hostUtilizationMips = usedMIPS.get(hostId);
		double hostPotentialUtilizationMips = hostUtilizationMips + requestedTotalMips;
		double pePotentialUtilization = hostPotentialUtilizationMips / host.getTotalMips();
		return pePotentialUtilization;
	}


	/**
	 * Releases the host used by a VM.
	 * 
	 * @param vm the vm
	 * 
	 * @pre $none
	 * @post none
	 */
	@Override
	public void deallocateHostForVm(Vm vm) {
		Host host = getVmTable().remove(vm.getUid());
		int idx = getHostList().indexOf(host);
		int pes = getUsedPes().remove(vm.getUid());
		if (host != null) {
			host.vmDestroy(vm);
			getFreePes().set(idx, getFreePes().get(idx) + pes);
		}
	}

	/**
	 * Gets the host that is executing the given VM belonging to the given user.
	 * 
	 * @param vm the vm
	 * 
	 * @return the Host with the given vmID and userID; $null if not found
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Host getHost(Vm vm) {
		return getVmTable().get(vm.getUid());
	}

	/**
	 * Gets the host that is executing the given VM belonging to the given user.
	 * 
	 * @param vmId the vm id
	 * @param userId the user id
	 * 
	 * @return the Host with the given vmID and userID; $null if not found
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Host getHost(int vmId, int userId) {
		//Log.printLine("vmId: " + vmId + " userID " + userId);
		//Log.printLine("Vm Uid " + Vm.getUid(userId, vmId));
		//Log.printLine(getVmTable());
		return getVmTable().get(Vm.getUid(userId, vmId));
	}

	/**
	 * Gets the vm table.
	 * 
	 * @return the vm table
	 */
	public Map<String, Host> getVmTable() {
		return vmTable;
	}

	/**
	 * Sets the vm table.
	 * 
	 * @param vmTable the vm table
	 */
	protected void setVmTable(Map<String, Host> vmTable) {
		this.vmTable = vmTable;
	}

	/**
	 * Gets the used pes.
	 * 
	 * @return the used pes
	 */
	protected Map<String, Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * Sets the used pes.
	 * 
	 * @param usedPes the used pes
	 */
	protected void setUsedPes(Map<String, Integer> usedPes) {
		this.usedPes = usedPes;
	}

	/**
	 * Gets the free pes.
	 * 
	 * @return the free pes
	 */
	protected List<Integer> getFreePes() {
		return freePes;
	}

	/**
	 * Sets the free pes.
	 * 
	 * @param freePes the new free pes
	 */
	protected void setFreePes(List<Integer> freePes) {
		this.freePes = freePes;
	}
	
	protected void setVnetHostTable(Map<String, List<Integer>> vnetHostList) {
		this.vnetHostTable = vnetHostList;
	}
	
	protected Map<String, List<Integer>> getVnetHostTable(){
		return this.vnetHostTable;
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.VmAllocationPolicy#optimizeAllocation(double, cloudsim.VmList, double)
	 */
	@Override
	public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
		
		//return null; //change for different test scenarios
		
		List<VnetHost> overUtilizedHosts = getOverUtilizedHosts();
	

		printOverUtilizedHosts(overUtilizedHosts);

		saveAllocation();

		List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
		
		
		Log.printLine("Reallocation of VMs from the over-utilized hosts:");
		List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<VnetHost>(
				overUtilizedHosts));
		

		migrationMap.addAll(getMigrationMapFromUnderUtilizedHosts(overUtilizedHosts));

		restoreAllocation();

		return migrationMap;
		
		
		
	}
	
	protected List<Map<String, Object>> getMigrationMapFromUnderUtilizedHosts(
			List<VnetHost> overUtilizedHosts) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		List<PowerHost> switchedOffHosts = getSwitchedOffHosts();

		
		Set<PowerHost> excludedHostsForFindingUnderUtilizedHost = new HashSet<PowerHost>();
		excludedHostsForFindingUnderUtilizedHost.addAll(overUtilizedHosts);
		excludedHostsForFindingUnderUtilizedHost.addAll(switchedOffHosts);
		excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(migrationMap));

		
		Set<PowerHost> excludedHostsForFindingNewVmPlacement = new HashSet<PowerHost>();
		excludedHostsForFindingNewVmPlacement.addAll(overUtilizedHosts);
		excludedHostsForFindingNewVmPlacement.addAll(switchedOffHosts);

		int numberOfHosts = getHostList().size();

		while (true) {
			if (numberOfHosts == excludedHostsForFindingUnderUtilizedHost.size()) {
				break;
			}

			VnetHost underUtilizedHost = getUnderUtilizedHost(excludedHostsForFindingUnderUtilizedHost);
			if (underUtilizedHost == null) {
				break;
			}

			Log.printLine("Under-utilized host: host #" + underUtilizedHost.getId() + "\n");

			excludedHostsForFindingUnderUtilizedHost.add(underUtilizedHost);
			excludedHostsForFindingNewVmPlacement.add(underUtilizedHost);

			List<? extends Vm> vmsToMigrateFromUnderUtilizedHost = getVmsToMigrateFromUnderUtilizedHost(underUtilizedHost);
			if (vmsToMigrateFromUnderUtilizedHost.isEmpty()) {
				continue;
			}

			Log.print("Reallocation of VMs from the under-utilized host: ");
			if (!Log.isDisabled()) {
				for (Vm vm : vmsToMigrateFromUnderUtilizedHost) {
					Log.print(vm.getId() + " ");
				}
			}
			Log.printLine();
			List<Map<String, Object>> newVmPlacement = getNewVmPlacementFromUnderUtilizedHost(
					vmsToMigrateFromUnderUtilizedHost,
					excludedHostsForFindingNewVmPlacement);

			excludedHostsForFindingUnderUtilizedHost.addAll(extractHostListFromMigrationMap(newVmPlacement));

			migrationMap.addAll(newVmPlacement);
			Log.printLine();
		}

		return migrationMap;
	}
	
	protected void restoreAllocation() {
		for (Host host : getHostList()) {
			host.vmDestroyAll();
			host.reallocateMigratingInVms();
		}
		for (Map<String, Object> map : getSavedAllocation()) {
			Vm vm = (Vm) map.get("vm");
			PowerHost host = (PowerHost) map.get("host");
			if (!host.vmCreate(vm)) {
				Log.printLine("Couldn't restore VM #" + vm.getId() + " on host #" + host.getId());
				System.exit(0);
			}
			getVmTable().put(vm.getUid(), host);
		}
	}
	
	protected List<Map<String, Object>> getNewVmPlacementFromUnderUtilizedHost(
			List<? extends Vm> vmsToMigrate,
			Set<? extends Host> excludedHosts) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		PowerVmList.sortByCpuUtilization(vmsToMigrate);
		for (Vm vm : vmsToMigrate) {
			VnetVm vm1 = (VnetVm)vm;
			VnetHost allocatedHost = findHostForVm(vm1, excludedHosts);
			if (allocatedHost != null) {
				allocatedHost.vmCreate(vm);
				Log.printLine("VM #" + vm.getId() + " allocated to host #" + allocatedHost.getId());

				Map<String, Object> migrate = new HashMap<String, Object>();
				migrate.put("vm", vm);
				migrate.put("host", allocatedHost);
				migrationMap.add(migrate);
			} else {
				Log.printLine("Not all VMs can be reallocated from the host, reallocation cancelled");
				for (Map<String, Object> map : migrationMap) {
					((Host) map.get("host")).vmDestroy((Vm) map.get("vm"));
				}
				migrationMap.clear();
				break;
			}
		}
		return migrationMap;
	}
	
	protected List<? extends Vm> getVmsToMigrateFromUnderUtilizedHost(PowerHost host) {
		List<Vm> vmsToMigrate = new LinkedList<Vm>();
		for (Vm vm : host.getVmList()) {
			if (!vm.isInMigration()) {
				vmsToMigrate.add(vm);
			}
		}
		return vmsToMigrate;
	}

	
	protected VnetHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {
		double minUtilization = 0.1;
		VnetHost underUtilizedHost = null;
		for (VnetHost host : this.<VnetHost> getHostList()) {
			if (excludedHosts.contains(host)) {
				continue;
			}
			double utilization = host.getUtilizationOfCpu();
			if (utilization > 0 && utilization < minUtilization
					&& !areAllVmsMigratingOutOrAnyVmMigratingIn(host)) {
				minUtilization = utilization;
				underUtilizedHost = host;
			}
		}
		return underUtilizedHost;
	}
	
	protected boolean areAllVmsMigratingOutOrAnyVmMigratingIn(PowerHost host) {
		for (PowerVm vm : host.<PowerVm> getVmList()) {
			if (!vm.isInMigration()) {
				return false;
			}
			if (host.getVmsMigratingIn().contains(vm)) {
				return true;
			}
		}
		return true;
	}
	
	protected List<PowerHost> extractHostListFromMigrationMap(List<Map<String, Object>> migrationMap) {
		List<PowerHost> hosts = new LinkedList<PowerHost>();
		for (Map<String, Object> map : migrationMap) {
			hosts.add((PowerHost) map.get("host"));
		}
		return hosts;
	}
	
	protected List<PowerHost> getSwitchedOffHosts() {
		List<PowerHost> switchedOffHosts = new LinkedList<PowerHost>();
		for (PowerHost host : this.<PowerHost> getHostList()) {
			if (host.getUtilizationOfCpu() == 0) {
				switchedOffHosts.add(host);
			}
		}
		return switchedOffHosts;
	}
	
	protected boolean isHostOverUtilizedAfterAllocation(VnetHost host, Vm vm) {
		boolean isHostOverUtilizedAfterAllocation = true;
		if (host.vmCreate(vm)) {
			isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
			host.vmDestroy(vm);
		}
		return isHostOverUtilizedAfterAllocation;
	}
	
	protected double getUtilizationOfCpuMips(PowerHost host) {
		double hostUtilizationMips = 0;
		for (Vm vm2 : host.getVmList()) {
			if (host.getVmsMigratingIn().contains(vm2)) {
				
				hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
			}
			hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
		}
		return hostUtilizationMips;
	}
	
	public VnetHost findHostForVm(VnetVm vm, Set<? extends Host> excludedHosts) {
		double minPower = Double.MAX_VALUE;
		VnetHost allocatedHost = null;
		Map<Integer, Integer> vnetHostIds = new HashMap<>();
		if(vm.getVnet() != null) {
			for(HostPacket hp : vm.recvPktlist) {
				if(vnetHostIds.containsKey(hp.sender)) {
					int k = vnetHostIds.get(hp.sender);
					vnetHostIds.put(hp.sender, ++k);
					
				}else {
					vnetHostIds.put(hp.sender, 1);
				}
				
				
			}
		}

		for (VnetHost host : this.<VnetHost> getHostList()) {
		
			if (excludedHosts.contains(host)) {
				Log.printLine("Host " + host.getId() + " is in the excluded host List: " + excludedHosts);
				continue;
			}
			if (host.isSuitableForVm(vm)) {
				if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
					Log.printLine("Host " + host.getId() + " is either switched off or over allocated.");
					continue;
				}

				try {
					double powerAfterAllocation = getPowerAfterAllocation(host,host.getId(), vm);
			
					if (powerAfterAllocation != -1) {
						
						
			
						double powerDiff = powerAfterAllocation - host.getPower();
						Log.printLine("Packet cost from host " + host.getId() + " to vm " + vm.getId() + " is " + getPowerofPacketTrace(host, vm, vnetHostIds));
						if (powerDiff + getPowerofPacketTrace(host, vm, vnetHostIds)< minPower) {
							minPower = powerDiff+ getPowerofPacketTrace(host, vm, vnetHostIds);
							allocatedHost = host;
						}
					}
				} catch (Exception e) {
					Log.printLine("Exception " + e.getStackTrace());
				}
			}else {
				Log.printLine("Host " + host.getId() + "is not suitable for VM " + vm.getId());
			}
			
		}
		return allocatedHost;
	}
	
	private double getPowerofPacketTrace(VnetHost vhFrom, VnetVm vm2, Map<Integer, Integer> vnetHostIds){
		if(vhFrom.getVmList().contains(vm2)) {
			return 0;
		}
		int edgeCount = 0;
		int aggCount = 0;
		int rootCount = 0;
		int pktCount = 0;
		double sumPowerPacket = 0;
		for(Host vh : getHostList()) {
			VnetHost vh1 = (VnetHost)vh;
			if(vnetHostIds.containsKey(vh1.getId())){
				pktCount = vnetHostIds.get(vh1.getId());
				if(vhFrom.sw == vh1.sw) {
					//sumPowerPacket += 0.000002 * pktCount;
					sumPowerPacket += 7.2 * pktCount / 50;
				}else {
					if(vhFrom.sw.uplinkswitches.get(0) == vh1.sw.uplinkswitches.get(0)) {
						//sumPowerPacket += (0.00001 + 0.000004) * pktCount;
						sumPowerPacket += (36 + 7.2) * pktCount / 50;
					}else {
						//sumPowerPacket += (0.000001 + 0.00002 + 0.000004) * pktCount;
						sumPowerPacket += (36 + 3.6 + 7.2) * pktCount / 50;
					}
				}
			}
			
		}
		return sumPowerPacket;
		
		
	}
	
	protected List<Map<String, Object>> getNewVmPlacement(
			List<? extends Vm> vmsToMigrate,
			Set<? extends Host> excludedHosts) {
		List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
		PowerVmList.sortByCpuUtilization(vmsToMigrate);
		for (Vm vm : vmsToMigrate) {
			VnetVm vm1 = (VnetVm)vm;
			VnetHost allocatedHost = findHostForVm(vm1, excludedHosts);
			if (allocatedHost != null) {
				allocatedHost.vmCreate(vm);
				Log.printLine("VM #" + vm.getId() + " allocated to host #" + allocatedHost.getId());

				Map<String, Object> migrate = new HashMap<String, Object>();
				migrate.put("vm", vm);
				migrate.put("host", allocatedHost);
				migrationMap.add(migrate);
			}else {
				Log.printLine("No free host found for VM reallocation");
			}
		}
		return migrationMap;
	}
	
	protected List<? extends Vm> getVmsToMigrateFromHosts(List<VnetHost> overUtilizedHosts) {
		List<Vm> vmsToMigrate = new LinkedList<Vm>();
		for (VnetHost host : overUtilizedHosts) {
			double alreadyDEstroyedMips = 0;
			while (true) {
				Vm vm = getVmSelectionPolicy().getVmToMigrate(host);
				if (vm == null) {
					break;
				}
				VnetVm vm1 = (VnetVm)vm;
				alreadyDEstroyedMips += vm.getMips();
				vmsToMigrate.add(vm);
				host.vmDestroy(vm);
				if (!isHostOverUtilizedAfterRemoval(host,vm1, alreadyDEstroyedMips)) {
					break;
				}
			}
		}
		return vmsToMigrate;
	}
	
	protected VnetVmSelectionPolicy getVmSelectionPolicy() {
		return vmSelectionPolicy;
	}
	
	protected List<Map<String, Object>> getSavedAllocation() {
		return savedAllocation;
	}
	
	protected void saveAllocation() {
		getSavedAllocation().clear();
		for (Host host : getHostList()) {
			for (Vm vm : host.getVmList()) {
				if (host.getVmsMigratingIn().contains(vm)) {
					continue;
				}
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("host", host);
				map.put("vm", vm);
				getSavedAllocation().add(map);
			}
		}
	}
	
	protected List<VnetHost> getOverUtilizedHosts() {
		List<VnetHost> overUtilizedHosts = new LinkedList<VnetHost>();
		for (VnetHost host : this.<VnetHost> getHostList()) {
			if (isHostOverUtilized(host)) {
				overUtilizedHosts.add(host);
			}
		}
		return overUtilizedHosts;
	}

	
	protected boolean isHostOverUtilized(VnetHost host){
		double maxPower = getMaxPowerOfHost(host);
		double usedPower = getPowerOfHost(host);
		double utilization = host.getUtilizationOfCpu();
		Log.printLine("Host " + host.getId() + " Max Power " + maxPower + " current Power " + usedPower);
		if(maxPower * 0.95 <= usedPower || utilization > 0.95) {
			return true;
		}
		return false;
	
	}
	
	protected boolean isHostOverUtilizedAfterRemoval(VnetHost host, VnetVm vm, double already){
		double maxPower = getMaxPowerOfHost(host);
		double usedPower = getPowerOfHost(host);
		double mipsUtilHost =host.getUtilizationOfCpuMips();
		double maxUtilHost = host.getTotalMips();
		double vmMips = vm.getMips();
		double utilization = host.getUtilizationOfCpu();
		Log.printLine("Host " + host.getId() + " Max Mips " + maxUtilHost + " current vm mips " + vmMips + " current hostt mips " + mipsUtilHost);
		if(mipsUtilHost - vmMips - already > maxUtilHost *0.9) {
			return true;
		}
		return false;
		
	}
	
	protected void printOverUtilizedHosts(List<VnetHost> overUtilizedHosts) {
		if (!Log.isDisabled()) {
			Log.printLine("Over-utilized hosts:");
			for (VnetHost host : overUtilizedHosts) {
				Log.printLine("Host #" + host.getId());
			}
			Log.printLine();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm,
	 * org.cloudbus.cloudsim.Host)
	 */
	@Override
	public boolean allocateHostForVm(Vm vm, Host host) {
		if (host.vmCreate(vm)) { // if vm has been succesfully created in the host
			getVmTable().put(vm.getUid(), host);

			int requiredPes = vm.getNumberOfPes();
			int idx = getHostList().indexOf(host);
			getUsedPes().put(vm.getUid(), requiredPes);
			getFreePes().set(idx, getFreePes().get(idx) - requiredPes);

			Log.formatLine(
					"%.2f: VM #" + vm.getId() + " has been allocated to the host #" + host.getId(),
					CloudSim.clock());
			return true;
		}

		return false;
	}
}
