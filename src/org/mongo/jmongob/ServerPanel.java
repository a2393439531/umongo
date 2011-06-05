/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mongo.jmongob;

import com.edgytech.swingfast.EnumListener;
import com.edgytech.swingfast.SwingFast;
import com.edgytech.swingfast.Text;
import com.edgytech.swingfast.XmlComponentUnit;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.mongo.jmongob.ServerPanel.Item;

/**
 *
 * @author antoine
 */
public class ServerPanel extends BasePanel implements EnumListener<Item> {

    enum Item {

        icon,
        host,
        address,
        maxObjectSize,
        durability,
        replication,
        clientPorts,
        serverStatus,
        replicaSetStatus,
        replicaSetInfo,
        refresh,
        rsStepDown,
        isMaster
    }

    public ServerPanel() {
        setEnumBinding(Item.values(), this);
    }

    public ServerNode getServerNode() {
        return (ServerNode) getNode();
    }

    @Override
    protected void updateComponentCustom(JPanel comp) {        
        try {
            Mongo svrMongo = getServerNode().getServerMongo();
            ServerAddress addr = getServerNode().getServerAddress();
            setStringFieldValue(Item.host, addr.toString());
            setStringFieldValue(Item.address, addr.getSocketAddress().toString());

            CommandResult res = svrMongo.getDB("local").command("isMaster");
            boolean master = res.getBoolean("ismaster");
            String replication = MongoUtils.makeInfoString("master", master,
                    "secondary", res.getBoolean("secondary"),
                    "passive", res.getBoolean("passive"));
            setStringFieldValue(Item.replication, replication);
            ((Text)getBoundUnit(Item.replication)).showIcon = master;

            setStringFieldValue(Item.maxObjectSize, String.valueOf(svrMongo.getMaxBsonObjectSize()));

            ((CmdField) getBoundUnit(Item.serverStatus)).updateFromCmd(svrMongo);

            DBObject svrStatus = ((DocField) getBoundUnit(Item.serverStatus)).getDoc();
            boolean dur = svrStatus.containsField("dur");
            ((Text)getBoundUnit(Item.durability)).setStringValue(dur ? "On" : "Off");
            ((Text)getBoundUnit(Item.durability)).showIcon = dur;
        } catch (Exception e) {
            JMongoBrowser.instance.showError(this.getClass().getSimpleName() + " update", e);
        }
    }

    public void actionPerformed(Item enm, XmlComponentUnit unit, Object src) {
    }

    public void rsStepDown() {
        final DB db = getServerNode().getServerMongo().getDB("admin");
        new DbJob() {

            @Override
            public Object doRun() throws IOException {
                return db.command("replSetStepDown");
            }

            @Override
            public String getNS() {
                return db.getName();
            }

            @Override
            public String getShortName() {
                return "RS Step Down";
            }
        }.addJob();
    }

    public void serverStatus() {
        new DocView(null, "Server Status", getServerNode().getServerMongo().getDB("admin"), "serverStatus").addToTabbedDiv();
    }

    public void isMaster() {
        new DocView(null, "Is Master", getServerNode().getServerMongo().getDB("admin"), "isMaster").addToTabbedDiv();
    }

    public void replicaSetStatus() {
        new DocView(null, "Server Status", getServerNode().getServerMongo().getDB("admin"), "replSetGetStatus").addToTabbedDiv();
    }

    public void replicaSetInfo() {
        new DocView(null, "RS Info", MongoUtils.getReplicaSetInfo(getServerNode().getServerMongo()), null, null).addToTabbedDiv();
    }
}