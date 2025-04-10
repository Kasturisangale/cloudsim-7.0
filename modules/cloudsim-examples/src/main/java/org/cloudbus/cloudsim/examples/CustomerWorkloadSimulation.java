package org.cloudbus.cloudsim.examples;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

public class CustomerWorkloadSimulation {
    private static final int[][] IOT_DEVICES = {
        {1, 50, 5000, 8},  // Device ID, Data Size (MB), Processing Demand (MI), Latency Sensitivity
        {2, 30, 3000, 5},
        {3, 100, 8000, 9}
    };

    private final CloudSimPlus simulation;
    private DatacenterBroker broker;
    private List<Vm> edgeServers;
    private List<Cloudlet> iotTasks;

    public static void main(String[] args) {
        new CustomerWorkloadSimulation();
    }

    public CustomerWorkloadSimulation() {
        System.out.println("Starting Edge Computing Simulation");
        simulation = new CloudSimPlus();
        createEdgeDataCenter();
        broker = new DatacenterBrokerSimple(simulation);
        edgeServers = createEdgeServers();
        iotTasks = createIoTasks();
        broker.submitVmList(edgeServers);
        broker.submitCloudletList(iotTasks);
        simulation.start();
        printResults();
    }

    private Datacenter createEdgeDataCenter() {
        List<Host> hostList = new ArrayList<>();
        int hostCount = 3;
        int peCount = 4;
        long mips = 1000;
        long ram = 8192;  // 8GB RAM
        long storage = 1000000;
        long bw = 10000;  // 10Gbps

        for (int i = 0; i < hostCount; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < peCount; j++) {
                peList.add(new PeSimple(mips));
            }
            Host host = new HostSimple(ram, bw, storage, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            hostList.add(host);
        }
        return new DatacenterSimple(simulation, hostList);
    }

    private List<Vm> createEdgeServers() {
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Vm vm = new VmSimple(i, 1000, 2)
                .setRam(4096)
                .setBw(1000)
                .setSize(5000)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        return vmList;
    }

    private List<Cloudlet> createIoTasks() {
        List<Cloudlet> taskList = new ArrayList<>();
        for (int[] device : IOT_DEVICES) {
            UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
            Cloudlet task = new CloudletSimple(device[0], device[2], 1)
                .setFileSize(device[1] * 1024)
                .setOutputSize(500)
                .setUtilizationModelCpu(utilizationModel);
            task.setVm(edgeServers.get(device[0] % edgeServers.size()));  // Round-robin assignment
            taskList.add(task);
        }
        return taskList;
    }

    private void printResults() {
        List<Cloudlet> finishedTasks = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedTasks).build();

        double totalLatency = finishedTasks.stream()
            .mapToDouble(task -> task.getFinishTime() - task.getStartTime())
            .sum();
        System.out.printf("\nAverage Latency: %.2f sec\n", totalLatency / finishedTasks.size());
    }
}