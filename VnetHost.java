
package cloudsim.grzegorz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.network.datacenter.HostPacket;
import org.cloudbus.cloudsim.network.datacenter.NetworkCloudletSpaceSharedScheduler;
import org.cloudbus.cloudsim.network.datacenter.NetworkConstants;
import org.cloudbus.cloudsim.network.datacenter.NetworkHost;
import org.cloudbus.cloudsim.network.datacenter.NetworkPacket;
import org.cloudbus.cloudsim.network.datacenter.NetworkVm;
import org.cloudbus.cloudsim.network.datacenter.Switch;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.network.datacenter.EdgeSwitch;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * VnetHost 
 * 
 * based on NetworkHost by
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 3.0
 *  */
public class VnetHost extends PowerHost{
	
	public List<NetworkPacket> packetTosendLocal;

	public List<NetworkPacket> packetTosendGlobal;

	public List<NetworkPacket> packetrecieved;

	public double memory;

	public Switch sw; // Edge switch in general

	public double bandwidth;// latency

	
	public List<Double> CPUfinTimeCPU = new ArrayList<Double>();

	public double fintime = 0;

	public Map<Integer, String> VmToVnet;

	public VnetHost(
			int id,
			RamProvisioner ramProvisioner,
			BwProvisioner bwProvisioner,
			long storage,
			List<? extends Pe> peList,
			VmScheduler vmScheduler,
			PowerModel powerModel) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
		
		VmToVnet = new HashMap<Integer, String>();
		packetrecieved = new ArrayList<NetworkPacket>();
		packetTosendGlobal = new ArrayList<NetworkPacket>();
		packetTosendLocal = new ArrayList<NetworkPacket>();
		

	}
	
	protected double[] getUtilizationHistory() {
		double[] utilizationHistory = new double[PowerVm.HISTORY_LENGTH];
		double hostMips = getTotalMips();
		for (VnetVm vm : this.<VnetVm> getVmList()) {
			for (int i = 0; i < vm.getUtilizationHistory().size(); i++) {
				utilizationHistory[i] += vm.getUtilizationHistory().get(i) * vm.getMips() / hostMips;
			}
		}
		return MathUtil.trimZeroTail(utilizationHistory);
	}
	
	
	@Override
	public double updateVmsProcessing(double currentTime) {
		
		double smallerTime = super.updateVmsProcessing(currentTime);
		recvpackets();
		for (Vm vm : super.getVmList()) {
			double time = ((VnetVm) vm).updateVmProcessing(currentTime, getVmScheduler()
					.getAllocatedMipsForVm(vm));
			if (time > 0.0 && time < smallerTime) {
				smallerTime = time;
			}
		}
		sendpackets();

		return smallerTime;

	}
	
	private void sendPingRequestHost(int destHostId) {
		EdgeSwitch esw = (EdgeSwitch) this.sw;
		
	}
	
	private void recvpackets() {

		for (NetworkPacket hs : packetrecieved) {
			hs.pkt.recievetime = CloudSim.clock();
			Log.printLine("Received packet " + hs.pkt + " Host Id: " + getId());
			// insertthe packet in recievedlist of VM
			Vm vm = VmList.getById(getVmList(), hs.pkt.reciever);
			if(vm != null) {
			
				VnetVm vm1 = (VnetVm) vm;
				Log.printLine("Recv packet Lsit: " + ((NetworkCloudletSpaceSharedScheduler) vm.getCloudletScheduler()));
				List<HostPacket> pktlist = ((NetworkCloudletSpaceSharedScheduler) vm.getCloudletScheduler()).pktrecv
						.get(hs.pkt.sender);
				vm1.getRecvPktlist().add(hs.pkt);

				if (pktlist == null) {
					pktlist = new ArrayList<HostPacket>();
					((NetworkCloudletSpaceSharedScheduler) vm.getCloudletScheduler()).pktrecv.put(
						hs.pkt.sender,
						pktlist);

				}
				pktlist.add(hs.pkt);
			}

		}
		packetrecieved.clear();
	}
	
	private void sendpackets() {

		for (Vm vm : super.getVmList()) {
			for (Entry<Integer, List<HostPacket>> es : ((NetworkCloudletSpaceSharedScheduler) vm
					.getCloudletScheduler()).pkttosend.entrySet()) {
				List<HostPacket> pktlist = es.getValue();
				for (HostPacket pkt : pktlist) {
					NetworkPacket hpkt = new NetworkPacket(getId(), pkt, vm.getId(), pkt.sender);
				
					Vm vm2 = VmList.getById(this.getVmList(), hpkt.recievervmid);
					if (vm2 != null) {
						packetTosendLocal.add(hpkt);
					} else {
						packetTosendGlobal.add(hpkt);
					}
				}
				pktlist.clear();

			}

		}

		boolean flag = false;

		for (NetworkPacket hs : packetTosendLocal) {
			flag = true;
			hs.stime = hs.rtime;
			hs.pkt.recievetime = CloudSim.clock();
			
			Vm vm = VmList.getById(getVmList(), hs.pkt.reciever);

			List<HostPacket> pktlist = ((NetworkCloudletSpaceSharedScheduler) vm.getCloudletScheduler()).pktrecv
					.get(hs.pkt.sender);
			if (pktlist == null) {
				pktlist = new ArrayList<HostPacket>();
				((NetworkCloudletSpaceSharedScheduler) vm.getCloudletScheduler()).pktrecv.put(
						hs.pkt.sender,
						pktlist);
			}
			pktlist.add(hs.pkt);
			packetrecieved.add(hs);

		}
		if (flag) {
			for (Vm vm : super.getVmList()) {
				vm.updateVmProcessing(CloudSim.clock(), getVmScheduler().getAllocatedMipsForVm(vm));
			}
		}

		
		packetTosendLocal.clear();
		double avband = bandwidth / packetTosendGlobal.size();
		for (NetworkPacket hs : packetTosendGlobal) {
			double delay = (1000 * hs.pkt.data) / avband;
			NetworkConstants.totaldatatransfer += hs.pkt.data;

			CloudSim.send(getDatacenter().getId(), sw.getId(), delay, CloudSimTags.Network_Event_UP, hs);

		}
		packetTosendGlobal.clear();
	}

	public double getMaxUtilizationAmongVmsPes(Vm vm) {
		return PeList.getMaxUtilizationAmongVmsPes(getPeList(), vm);
	}
	
	public Map<Integer, String> getVmToVnetList() {
		return this.VmToVnet;
	}
	
	public void setVmToVnetList(Map<Integer, String> list) {
		this.VmToVnet = list;
	}
	
}
