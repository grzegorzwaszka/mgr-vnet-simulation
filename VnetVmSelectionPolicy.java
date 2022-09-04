package cloudsim.grzegorz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

/**
 * VnetVmSelectionPolicy
 * 
 * based on PowerVmSelectionPolicy by
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class VnetVmSelectionPolicy {

	/**
	 * Gets the vms to migrate.
	 * 
	 * @param host the host
	 * @return the vms to migrate
	 */
	public VnetVm getVmToMigrate(VnetHost host) {
		List<VnetVm> migratableVms = getMigratableVms(host);
		if (migratableVms.isEmpty()) {
			return null;
		}
		VnetVm vmToMigrate = null;
		double minMetric = Double.MAX_VALUE;
		Map<String, List<VnetVm>> vnetList = new HashMap<>();
		for(VnetVm vm : migratableVms) {
			if(vm.getVnet() != null) {
				if(vnetList.get(vm.getVnet()) == null) {
					ArrayList<VnetVm> l = new ArrayList<>();
					l.add(vm);
					vnetList.put(vm.getVnet(), l);
				}else {
					vnetList.get(vm.getVnet()).add(vm);
				}
			}
		}
		VnetVm lastRemovedVm = null;
		for(Map.Entry<String, List<VnetVm>> entry : vnetList.entrySet()) {
			if(entry.getValue().size() > 1) {
				for(VnetVm vm : entry.getValue()) {
					lastRemovedVm = vm;
					migratableVms.remove(vm);
				}
			}
		}
		for (VnetVm vm : migratableVms) {
			if (vm.isInMigration()) {
				continue;
			}
			double metric = vm.getTotalUtilizationOfCpuMips(CloudSim.clock()) / vm.getMips();
			if (metric < minMetric) {
				minMetric = metric;
				vmToMigrate = vm;
			}
		}
		if(vmToMigrate == null) {
			if(lastRemovedVm != null) {
				vmToMigrate=lastRemovedVm;
			}
		}
		Log.printLine("Selected Vm To Migrate: " + vmToMigrate.getId());
		return vmToMigrate;
	}

	/**
	 * Gets the migratable vms.
	 * 
	 * @param host the host
	 * @return the migratable vms
	 */
	protected List<VnetVm> getMigratableVms(VnetHost host) {
		List<VnetVm> migratableVms = new ArrayList<VnetVm>();
		for (VnetVm vm : host.<VnetVm> getVmList()) {
			if (!vm.isInMigration()) {
				migratableVms.add(vm);
			}
		}
		return migratableVms;
	}

}