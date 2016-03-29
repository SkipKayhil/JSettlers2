/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2009,2011-2014 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.client;

import soc.game.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * This is the universal dialog to notify players that their
 * input is requested. Currently applies to discards, gaining
 * cards, trades being offered, and when someones turn begins.
 *<P>
 * For convenience with {@link java.awt.EventQueue#invokeLater(Runnable)},
 * contains a {@link #run()} method which calls {@link #setVisible(boolean) setVisible(true)}.
 *<P>
 * Before v2.0.00, this was called {@code SOCDiscardDialog},
 * and Year of Plenty used {@code SOCDiscoveryDialog}.
 * Before v2.0.01, this was called {@code SOCDiscardOrGainResDialog}
 *
 * @author  Robert S. Thomas and SkipKayhil
 */
@SuppressWarnings("serial")
class SOCNotificationDialog extends JDialog implements ActionListener, PropertyChangeListener, Runnable
{

    /** i18n text strings; will use same locale as SOCPlayerClient's string manager.
     *  @since 2.0.00 */
    private static final soc.util.SOCStringManager strings = soc.util.SOCStringManager.getClientManager();

    /**
     * Does the player have a dev card?
     * @since 2.0.00
     */
    private final boolean hasDev;

    String msg;
    String value;
    String rollButtonString = "Roll";
    String devButtonString = "Play Card";

    String[] inventoryList;

    Object[] content;
    Object[] butText;

    SOCPlayerInterface playerInterface;
    SOCInventory inventory;
    SOCGame game;
    SOCHandPanel hp;

    JOptionPane optionPane;
    //JList<String> inventorySelection;
    JScrollPane scrollPane;

    /** Desired size (visible size inside of insets) **/
    //protected int wantW, wantH;

    /**
     * Place window in center when displayed (in doLayout),
     * don't change position afterwards
     */
    boolean didSetLocation;

    /**
     * Creates a new SOCDiscardOrGainResDialog popup.
     * To show it on screen and make it active,
     * call {@link #setVisible(boolean) setVisible(true)}.
     *
     * @param pi   Client's player interface
     * @param inventory
     */
    public SOCNotificationDialog(SOCGame game, SOCPlayerInterface pi, SOCInventory inventory)
    {
        super(pi,strings.get("dialog.notification.title.turn",pi.getClient().getNickname()) ,true);

        playerInterface = pi;
        this.inventory = inventory;
        this.game = game;
        hp = playerInterface.getClientHand();
        hasDev = inventory.getAmount() > 0;

        msg = (hasDev ? "Play a Development Card or Roll the Dice" : "Roll the dice!");
        butText = (hasDev ? new String[]{rollButtonString, devButtonString} : new String[]{rollButtonString});

        if(hasDev){
            inventoryList = new String[inventory.getAmount()];

            for(int i = 0; i < inventoryList.length; i++){
                inventoryList[i] = inventory.getFullInventory()[i].getItemName(game, false, strings);
            }

            scrollPane = new JScrollPane(new JList<String>(inventoryList));

            content = new Object[]{msg, scrollPane};
        }else{
            content = new Object[]{msg};
        }

        optionPane = new JOptionPane(
                content,
                JOptionPane.PLAIN_MESSAGE,
                hasDev ? JOptionPane.YES_NO_OPTION : JOptionPane.DEFAULT_OPTION,
                null,
                butText,
                ""
        );

        setContentPane(optionPane);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        didSetLocation = false;
        setLayout(null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();

        if(isVisible()
                && (evt.getSource() == optionPane)
                && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
                JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();

            if (value == ""){
                playerInterface.getPlayerHandPanel(playerInterface.getClientPlayerNumber()).clickRollButton();
                setVisible(false);
            }else{
                for(String s: inventoryList){
                    if(value == s){
                        playerInterface.getClientHand().inventory.select(java.util.Arrays.binarySearch(inventoryList, value));
                        playerInterface.getClientHand().clickPlayCardButton();
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void run() {
        setVisible(true);
    }
}
