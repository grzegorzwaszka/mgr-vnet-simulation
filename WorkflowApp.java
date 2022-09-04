/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.network.datacenter;

import java.util.ArrayList;
import java.util.List;
import cloudsim.grzegorz.VnetVm;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * CHANGED
 * Method createCloudletList changed to include creating vms in the same virtual network
 * 
 * ORIGINAL
 * WorkflowApp is an example of AppCloudlet having three communicating tasks. Task A and B sends the
 * data (packet) while Task C receives them
 * 
 * Please refer to following publication for more details:
 * 
 * Saurabh Kumar Garg and Rajkumar Buyya, NetworkCloudSim: Modelling Parallel Applications in Cloud
 * Simulations, Proceedings of the 4th IEEE/ACM International Conference on Utility and Cloud
 * Computing (UCC 2011, IEEE CS Press, USA), Melbourne, Australia, December 5-7, 2011.
 * 
 * @author Saurabh Kumar Garg
 * @since CloudSim Toolkit 1.0
 */
public class WorkflowApp extends AppCloudlet {

	public WorkflowApp(int type, int appID, double deadline, int numbervm, int userId) {
		super(type, appID, deadline, numbervm, userId);
		exeTime = 100;
		//this.numbervm = 3;
	}

	public void createCloudletList(List<Integer> vmIdList, NetworkDatacenter linkDC) {
		//Find vnet for current VM
		long fileSize = NetworkConstants.FILE_SIZE;
		long outputSize = NetworkConstants.OUTPUT_SIZE;
		int memory = 100;
		UtilizationModel utilizationModel = new UtilizationModelFull();
		int i = 0;
		for(int vmId : vmIdList) {
			ArrayList<Integer> vmVnetList = new ArrayList<Integer>();
			String currVnet = ((VnetVm)linkDC.getVmList().get(vmId)).getVnet();
			for(Vm vm : linkDC.getVmList()) {
				VnetVm vm1 = (VnetVm) vm;
				if(vm.getId() != vmId) {
					if(vm1.getVnet() == currVnet) {
						vmVnetList.add(vm1.getId());
					}
				}
			}
			
			if(vmVnetList.size() > 0) {
				NetworkCloudlet cl = new NetworkCloudlet(
						NetworkConstants.currentCloudletId,
						0,
						1,
						fileSize,
						outputSize,
						memory,
						utilizationModel,
						utilizationModel,
						utilizationModel);
				cl.numStage = 2;
				NetworkConstants.currentCloudletId++;
				cl.setUserId(userId);
				cl.submittime = CloudSim.clock();
				cl.currStagenum = -1;
				cl.setVmId(vmId);
				// first stage: big computation
				cl.stages.add(new TaskStage(NetworkConstants.EXECUTION, 0, 1000 * 0.8, 0, memory, vmId, cl
						.getCloudletId()));
				cl.stages.add(new TaskStage(NetworkConstants.WAIT_SEND, 1000, 0, 1, memory, vmVnetList.get(0), cl
						.getCloudletId() + 1));
				clist.add(cl);
				NetworkCloudlet clc = new NetworkCloudlet(
						NetworkConstants.currentCloudletId,
						0,
						1,
						fileSize,
						outputSize,
						memory,
						utilizationModel,
						utilizationModel,
						utilizationModel);
				clc.numStage = 2;
				NetworkConstants.currentCloudletId++;
				clc.setUserId(userId);
				clc.submittime = CloudSim.clock();
				clc.currStagenum = -1;
				clc.setVmId(vmVnetList.get(0));

				// first stage: big computation
				clc.stages.add(new TaskStage(NetworkConstants.WAIT_RECV, 1000, 0, 0, memory, vmId, cl
						.getCloudletId()));
				clc.stages.add(new TaskStage(
						NetworkConstants.EXECUTION,
						0,
						1000 * 0.8,
						1,
						memory,
						vmId,
						clc.getCloudletId()));

				clist.add(clc);
			}
			
			
		}
		
	}
	
	@Override
	public void createCloudletList(List<Integer> vmIdList) {
		
		
		
	}
}
