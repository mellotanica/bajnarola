/*****************************************************************************/
/* Server package for Bajnarola distributed game                             */
/*                                                                           */
/* Copyright (C) 2015                                                        */
/* Marco Melletti, Davide Berardi, Matteo Martelli                           */
/*                                                                           */
/* This program is free software; you can redistribute it and/or             */
/* modify it under the terms of the GNU General Public License               */
/* as published by the Free Software Foundation; either version 2            */
/* of the License, or any later version.                                     */
/*                                                                           */
/* This program is distributed in the hope that it will be useful,           */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of            */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             */
/* GNU General Public License for more details.                              */
/*                                                                           */
/* You should have received a copy of the GNU General Public License         */
/* along with this program; if not, write to the Free Software               */
/* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,*/
/* USA.                                                                      */
/*****************************************************************************/

package org.bajnarola.game;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import org.bajnarola.game.controller.GameControllerRemote;
import org.bajnarola.networking.NetPlayer;
import org.bajnarola.utils.BajnarolaRegistry;
import org.bajnarola.utils.RandomString;

public class BajnarolaServer implements Remote {

	private static final int BIND_ATTEMPTS = 50;
	
	NetPlayer player = null;
	Registry registry = null;
	
	
	public NetPlayer getPlayer() {
		return this.player;
	}
	
	private int setRebind(String path, Remote o) throws Exception {
		String npath = path + "/" + o.getClass().getName();
		int port = BajnarolaRegistry.DEFAULT_PLAYER_PORT;
		Registry r = null;
		for (int i = 0; i < BIND_ATTEMPTS; i++) {
			try {
				r = BajnarolaRegistry.createRegistry(port);
				r.rebind(npath, o);
				break;
			} catch (RemoteException e) {
				System.err.println("Registry already bound on this port, trying with another port");
				port = 49152 + new Random().nextInt(65535 - 49152);
				if (i == BIND_ATTEMPTS - 1)
					throw new Exception("Too many bind attempts");
			}
		}
		
		this.registry = r;
		System.out.print("\n\tListening on '" + npath + "' ...");
		return port;
	}
	
	private void CommonConstruct(String basepath, GameControllerRemote myBoard) {
		int bindPort;
		try {
			bindPort = this.setRebind(basepath, myBoard);
			this.player = new NetPlayer(basepath, bindPort);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public BajnarolaServer(String basepath, GameControllerRemote myBoard) {
		String s = Integer.toString(new Random().nextInt());
;
		this.CommonConstruct(s + "/" + basepath, myBoard);
	}
	public BajnarolaServer(GameControllerRemote myBoard) {
		String s = Integer.toString(new Random().nextInt()) + "/" + RandomString.generateAsciiString();
		this.CommonConstruct(s, myBoard);
	}
	
	
	public void cleanRegistry(){
		try {
			this.registry.unbind(this.player.rmiUriBoard);
			UnicastRemoteObject.unexportObject(this.registry, true);
		} catch (RemoteException | NotBoundException e) {
			System.err.printf("object %s already unbound", this.player.rmiUriBoard);
		}
	}
}