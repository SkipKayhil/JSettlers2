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

import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


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
class SOCNotificationDialog extends Dialog implements ActionListener, MouseListener, Runnable
{

    /** i18n text strings; will use same locale as SOCPlayerClient's string manager.
     *  @since 2.0.00 */
    private static final soc.util.SOCStringManager strings = soc.util.SOCStringManager.getClientManager();

    /**
     * Clear button.  Reset the {@link #pick} resource colorsquare counts to 0.
     * @since 1.1.14
     */
    private JButton clearBut;

    /** Discard or Pick button */
    private JButton dopBut;

    /** Roll button */
    private JButton rollBut;

    /** The 'keep' square resource types/counts only change if {@link #notifReason} is reason.DISCARD. */
    ColorSquare[] keep;

    /** Resource types/counts to discard or gain */
    private ColorSquare[] pick;

    JLabel msg;
    JLabel youHave;
    JLabel dopThese;
    SOCPlayerInterface playerInterface;

    /** Must discard this many resources from {@link #keep}, or must gain this many resources. */
    private final int numPickNeeded;

    /**
     * Has chosen to discard or gain this many resources so far in {@link #pick}.
     * {@link #dopBut} is disabled unless proper number of resources ({@link #numPickNeeded}) are chosen.
     */
    private int numChosen;

    /** Desired size (visible size inside of insets) **/
    protected int wantW, wantH;

    /**
     * Place window in center when displayed (in doLayout),
     * don't change position afterwards
     */
    boolean didSetLocation;

    /**
     * Replaces boolean isDiscard with more dynamic options
     * @since 2.0.01
     */
    public enum reason {
        TURN, TRADE, DISCARD, GAIN
    }

    private reason notifReason;

    /**
     * Creates a new SOCDiscardOrGainResDialog popup.
     * To show it on screen and make it active,
     * call {@link #setVisible(boolean) setVisible(true)}.
     *
     * @param pi   Client's player interface
     * @param rnum Player must discard or gain this many resources
     * @param r  Reason for notification
     */
    public SOCNotificationDialog(SOCPlayerInterface pi, final int rnum, final reason r)
    {
        super(pi, true);

        notifReason = r;
        playerInterface = pi;
        numPickNeeded = rnum;
        numChosen = 0;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("SansSerif", Font.PLAIN, 12));

        msg = new JLabel();


        if(notifReason == SOCNotificationDialog.reason.DISCARD || notifReason == SOCNotificationDialog.reason.GAIN){
            dopThese = new JLabel();
            dopThese.setHorizontalAlignment(SwingConstants.LEFT);

            clearBut = new JButton(strings.get("base.clear"));

            dopBut = new JButton(strings.get(notifReason == SOCNotificationDialog.reason.DISCARD ? "dialog.discard.discard" : "dialog.discard.pick"));
            // "Discard" or "Pick"

            youHave = new JLabel(strings.get("dialog.discard.you.have"), SwingConstants.CENTER);  // "You have:"
        }

        switch(notifReason){
            case TURN:
                this.setTitle(strings.get("dialog.notification.title.turn",pi.getClient().getNickname()));
                msg.setText(strings.get("dialog.notification.message.turn"));
                //TODO autoroll
                break;
            case TRADE:
                this.setTitle(strings.get("dialog.notification.title.trade",pi.getClient().getNickname()));
                msg.setText(strings.get("dialog.notification.message.trade"));
                break;
            case DISCARD:
                this.setTitle(strings.get("dialog.notification.title.discard",pi.getClient().getNickname()));
                msg.setText(strings.get("dialog.notification.message.discard", numPickNeeded));
                // "Please discard {0} resources."
                dopThese.setText(strings.get("dialog.notification.these.discard"));
                // "Discard these:"
                break;
            case GAIN:
                this.setTitle(strings.get("dialog.notification.title.gain",pi.getClient().getNickname()));
                msg.setText(strings.get("dialog.notification.message.gain", numPickNeeded));
                // "Please pick {0} resources."
                dopThese.setText(strings.get("dialog.notification.these.gain"));
                // "Gain these:"
                break;
        }

        didSetLocation = false;
        setLayout(null);

        add(msg);

        if(notifReason == SOCNotificationDialog.reason.DISCARD || notifReason == SOCNotificationDialog.reason.GAIN){
            add(youHave);
            add(dopThese);

            add(clearBut);
            clearBut.addActionListener(this);
            clearBut.setEnabled(false);  // since nothing picked yet

            add(dopBut);
            dopBut.addActionListener(this);
            if (numPickNeeded > 0)
                dopBut.setEnabled(false);  // Must choose that many first

            keep = new ColorSquare[5];
            pick = new ColorSquare[5];

            for (int i = 0; i < 5; i++)
            {
                // On OSX: We must use the wrong color, then change it, in order to
                // not use AWTToolTips (redraw problem for button enable/disable).
                Color sqColor;
                if (SOCPlayerClient.isJavaOnOSX)
                    sqColor = Color.WHITE;
                else
                    sqColor = ColorSquare.RESOURCE_COLORS[i];

                keep[i] = new ColorSquareLarger(ColorSquare.BOUNDED_DEC, false, sqColor);
                pick[i] = new ColorSquareLarger(ColorSquare.BOUNDED_INC, false, sqColor);
                if (SOCPlayerClient.isJavaOnOSX)
                {
                    sqColor = ColorSquare.RESOURCE_COLORS[i];
                    keep[i].setBackground(sqColor);
                    pick[i].setBackground(sqColor);
                }
                add(keep[i]);
                add(pick[i]);
                keep[i].addMouseListener(this);
                pick[i].addMouseListener(this);
            }
        }

        // wantH formula based on doLayout
        //    labels: 20  colorsq: 20  button: 25  spacing: 5
        wantW = 270;
        wantH = 20 + 5 + (2 * (20 + 5 + 20 + 5)) + 25 + 5;   //this might be the funniest line in this whole application
        setSize(wantW + 10, wantH + 20);  // Can calc & add room for insets at doLayout


    }

    /**
     * Show or hide this dialog.
     * If showing (<tt>vis == true</tt>), also sets the initial values
     * of our current resources, based on {@link SOCPlayer#getResources()},
     * and requests focus on the Discard/Pick button.
     *
     * @param vis  True to make visible, false to hide
     */
    @Override
    public void setVisible(final boolean vis)
    {
        if (vis)
        {
            /**
             * set initial values
             */
            SOCPlayer player = playerInterface.getGame().getPlayer(playerInterface.getClient().getNickname());
            SOCResourceSet resources = player.getResources();
            keep[0].setIntValue(resources.getAmount(SOCResourceConstants.CLAY));
            keep[1].setIntValue(resources.getAmount(SOCResourceConstants.ORE));
            keep[2].setIntValue(resources.getAmount(SOCResourceConstants.SHEEP));
            keep[3].setIntValue(resources.getAmount(SOCResourceConstants.WHEAT));
            keep[4].setIntValue(resources.getAmount(SOCResourceConstants.WOOD));

            dopBut.requestFocus();
        }

        super.setVisible(vis);
    }

    /**
     * Custom layout, and setLocation call, for this dialog.
     */
    @Override
    public void doLayout()
    {
        int x = getInsets().left;
        int padW = getInsets().left + getInsets().right;
        int padH = getInsets().top + getInsets().bottom;
        int width = getSize().width - padW;
        int height = getSize().height - padH;

        /* check visible-size vs insets */
        if ((width < wantW + padW) || (height < wantH + padH))
        {
            if (width < wantW + padW)
                width = wantW + 1;
            if (height < wantH + padH)
                height = wantH + 1;
            setSize (width + padW, height + padH);
            width = getSize().width - padW;
            height = getSize().height - padH;
        }

        int space = 5;
        int msgW = this.getFontMetrics(this.getFont()).stringWidth(msg.getText());
        int sqwidth = ColorSquareLarger.WIDTH_L;
        int sqspace = (width - (5 * sqwidth)) / 6;

        int keepY;
        int discY;

        /* put the dialog in the center of the game window */
        if (! didSetLocation)
        {
            int cfx = playerInterface.getInsets().left;
            int cfy = playerInterface.getInsets().top;
            int cfwidth = playerInterface.getSize().width - playerInterface.getInsets().left - playerInterface.getInsets().right;
            int cfheight = playerInterface.getSize().height - playerInterface.getInsets().top - playerInterface.getInsets().bottom;

            final Point piLoc = playerInterface.getLocation();
            setLocation(piLoc.x + cfx + ((cfwidth - width) / 2), piLoc.y + cfy + ((cfheight - height) / 3));
            didSetLocation = true;
        }

        try
        {
            msg.setBounds((width - msgW) / 2, getInsets().top, msgW + 4, 20);
            final int btnsX = (getSize().width - (2 * 80 + 5)) / 2;
            int y = (getInsets().top + height) - 30;
            clearBut.setBounds(btnsX, y, 80, 25);
            dopBut.setBounds(btnsX + 85, y, 80, 25);
            youHave.setBounds(getInsets().left, getInsets().top + 20 + space, 70, 20);
            dopThese.setBounds(getInsets().left, getInsets().top + 20 + space + 20 + space + sqwidth + space, 100, 20);
        }
        catch (NullPointerException e) {}

        keepY = getInsets().top + 20 + space + 20 + space;
        discY = keepY + sqwidth + space + 20 + space;

        try
        {
            for (int i = 0; i < 5; i++)
            {
                keep[i].setSize(sqwidth, sqwidth);
                keep[i].setLocation(x + sqspace + (i * (sqspace + sqwidth)), keepY);
                pick[i].setSize(sqwidth, sqwidth);
                pick[i].setLocation(x + sqspace + (i * (sqspace + sqwidth)), discY);
            }
        }
        catch (NullPointerException e) {

        }
    }

    /**
     * React to clicking Discard/Pick button or Clear button.
     *<P>
     * ColorSquare clicks are handled in {@link #mousePressed(MouseEvent)}.
     *
     * @param e  ActionEvent for the click, with {@link ActionEvent#getSource()} == our button
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
        Object target = e.getSource();

        if (target == dopBut)
        {
            SOCResourceSet rsrcs = new SOCResourceSet(pick[0].getIntValue(), pick[1].getIntValue(), pick[2].getIntValue(), pick[3].getIntValue(), pick[4].getIntValue(), 0);

            if (rsrcs.getTotal() == numPickNeeded)
            {
                SOCPlayerClient pcli = playerInterface.getClient();
                if (notifReason == SOCNotificationDialog.reason.DISCARD)
                    pcli.getGameManager().discard(playerInterface.getGame(), rsrcs);
                else if(notifReason == SOCNotificationDialog.reason.GAIN)
                    pcli.getGameManager().pickResources(playerInterface.getGame(), rsrcs);
                dispose();
            }
        }
        else if (target == clearBut)
        {
            for (int i = pick.length - 1; i >= 0; --i)
            {
                if (notifReason == SOCNotificationDialog.reason.DISCARD)
                    keep[i].addValue(pick[i].getIntValue());
                pick[i].setIntValue(0);
            }
            numChosen = 0;
            clearBut.setEnabled(false);
            dopBut.setEnabled(numPickNeeded == numChosen);
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /** Stub, required for {@link MouseListener}. */
    public void mouseEntered(MouseEvent e)
    {
        ;
    }

    /** Stub, required for {@link MouseListener}. */
    public void mouseExited(MouseEvent e)
    {
        ;
    }

    /** Stub, required for {@link MouseListener}. */
    public void mouseClicked(MouseEvent e)
    {
        ;
    }

    /** Stub, required for {@link MouseListener}. */
    public void mouseReleased(MouseEvent e)
    {
        ;
    }

    /**
     * When a resource's colorsquare is clicked, add/remove 1
     * from the resource totals as requested; update {@link #dopBut}
     * and {@link #clearBut}.
     *<P>
     * If not isDiscard, will not subtract change the "keep" colorsquare resource counts.
     *<P>
     * If we only need 1 total, and we've picked one and
     * now pick a different one, zero the previous pick
     * and change our choice to the new one.
     *<P>
     * Clear/Discard button clicks are handled in {@link #actionPerformed(ActionEvent)}.
     */
    public void mousePressed(MouseEvent e)
    {
        try {
        Object target = e.getSource();
        boolean wantsRepaint = false;

        for (int i = 0; i < 5; i++)
        {
            if ((target == keep[i]) && (pick[i].getIntValue() > 0))
            {
                if (notifReason == SOCNotificationDialog.reason.DISCARD)
                    keep[i].addValue(1);
                pick[i].subtractValue(1);
                --numChosen;
                if (numChosen == (numPickNeeded-1))
                {
                    dopBut.setEnabled(false);  // Count un-reached (too few)
                    wantsRepaint = true;
                }
                else if (numChosen == numPickNeeded)
                {
                    dopBut.setEnabled(true);   // Exact count reached
                    wantsRepaint = true;
                }
                break;
            }
            else if ((target == pick[i]) && ((keep[i].getIntValue() > 0) || notifReason == SOCNotificationDialog.reason.GAIN))
            {
                if ((numPickNeeded == 1) && (numChosen == 1))
                {
                    // We only need 1 total, change our previous choice to the new one

                    if (pick[i].getIntValue() == 1)
                        return;  // <--- early return: already set to 1 ---
                    else
                        // clear all to 0
                        for (int j = 0; j < 5; ++j)
                        {
                            final int n = pick[j].getIntValue();
                            if (n == 0)
                                continue;
                            if (notifReason == SOCNotificationDialog.reason.DISCARD)
                                keep[j].addValue(n);
                            pick[j].subtractValue(n);
                        }

                    numChosen = 0;
                }

                if (notifReason == SOCNotificationDialog.reason.DISCARD)
                    keep[i].subtractValue(1);
                pick[i].addValue(1);
                ++numChosen;
                if (numChosen == numPickNeeded)
                {
                    dopBut.setEnabled(true);  // Exact count reached
                    wantsRepaint = true;
                }
                else if (numChosen == (numPickNeeded+1))
                {
                    dopBut.setEnabled(false);  // Count un-reached (too many)
                    wantsRepaint = true;
                }
                break;
            }
        }

        clearBut.setEnabled(numChosen > 0);

        if (wantsRepaint)
        {
            dopBut.repaint();
        }

        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * Run method, for convenience with {@link java.awt.EventQueue#invokeLater(Runnable)}.
     * This method just calls {@link #setVisible(boolean) setVisible(true)}.
     * @since 2.0.00
     */
    public void run()
    {
        setVisible(true);
    }

}
