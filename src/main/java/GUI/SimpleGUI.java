package GUI;

import blockchain.Block;
import blockchain.Item;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.swing.*;
import p2p.Key;
import p2p.Peer;
import p2p.protocol.InfectionMessage;

public class SimpleGUI extends JFrame implements ActionListener {
  private static final long serialVersionUID = 1L;

  // declare some things we need
  private JLabel introLbl, lbl1, lbl2, lbl3;
  private JTextField txtfld1, txtfld2, txtfld3;
  private JButton btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn10, btn11;
  private JTextArea txtArea1, txtArea2, txtArea3, txtArea4;
  public Peer p;

  public SimpleGUI(Peer p) {
    this.p = p;
  }

  public void start() {
    // make window object

    init(); // init all our things!

    // set window object size
    setSize(1200, 1000);
    setTitle(p.localNode.getId().toString());
    setVisible(true);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    Timer timer = new Timer();
    timer.schedule(new Task1(), 1000, 5000);
  }

  public void init() {
    // create container to hold GUI in window
    Container pane = this.getContentPane();
    pane.setLayout(null);

    // intro label
    introLbl = new JLabel();
    introLbl.setBounds(10, 10, 300, 20);
    introLbl.setText("Quick intro to app");

    //1
    lbl1 = new JLabel("ID Item");
    lbl1.setBounds(10, 30, 80, 20);
    txtfld1 = new JTextField();
    txtfld1.setBounds(70, 30, 100, 20);
    //2
    lbl2 = new JLabel("ID Value");
    lbl2.setBounds(10, 60, 80, 20);
    txtfld2 = new JTextField();
    txtfld2.setBounds(70, 60, 100, 20);

    // 3
    lbl3 = new JLabel("Subscribe");
    lbl3.setBounds(10, 90, 80, 20);
    txtfld3 = new JTextField();
    txtfld3.setBounds(70, 90, 100, 20);

    // generate button
    btn1 = new JButton("List Routing Table");
    btn1.setBounds(10, 200, 200, 20);
    btn1.addActionListener(this);

    btn2 = new JButton("List Local Stored Block");
    btn2.setBounds(10, 230, 200, 20);
    btn2.addActionListener(this);

    btn3 = new JButton("BID");
    btn3.setBounds(180, 30, 200, 20);
    btn3.addActionListener(this);

    btn7 = new JButton("---");
    btn7.setBounds(180, 60, 200, 20);
    btn7.addActionListener(this);

    btn8 = new JButton("Submit");
    btn8.setBounds(180, 90, 200, 20);
    btn8.addActionListener(this);

    btn4 = new JButton("Block Creation");
    btn4.setBounds(10, 260, 200, 20);
    btn4.addActionListener(this);

    btn5 = new JButton("List To Sell Items");
    btn5.setBounds(10, 290, 200, 20);
    btn5.addActionListener(this);

    btn6 = new JButton("Advertise Items");
    btn6.setBounds(10, 320, 200, 20);
    btn6.addActionListener(this);

    btn9 = new JButton("List Internal Int");
    btn9.setBounds(10, 350, 200, 20);
    btn9.addActionListener(this);

    btn10 = new JButton("List External Int");
    btn10.setBounds(10, 380, 200, 20);
    btn10.addActionListener(this);

    btn11 = new JButton("List Intersection Int");
    btn11.setBounds(10, 410, 200, 20);
    btn11.addActionListener(this);
    //text area output (with formatted font)
    txtArea1 = new JTextArea("");
    txtArea1.setEditable(false);
    txtArea1.setFont(new Font("monospaced", Font.PLAIN, 12));
    JScrollPane scroll = new JScrollPane(txtArea1);
    scroll.setBounds(590, 20, 570, 400);

    txtArea2 = new JTextArea("");
    txtArea2.setEditable(false);
    txtArea2.setFont(new Font("monospaced", Font.ROMAN_BASELINE, 12));
    JScrollPane scroll1 = new JScrollPane(txtArea2);
    scroll1.setBounds(10, 450, 570, 190);

    txtArea4 = new JTextArea("");
    txtArea4.setEditable(false);
    txtArea4.setFont(new Font("monospaced", Font.ROMAN_BASELINE, 12));
    JScrollPane scroll3 = new JScrollPane(txtArea4);
    scroll3.setBounds(10, 650, 570, 190);

    txtArea3 = new JTextArea("");
    txtArea3.setEditable(false);
    txtArea3.setFont(new Font("monospaced", Font.ROMAN_BASELINE, 12));
    JScrollPane scroll2 = new JScrollPane(txtArea3);
    scroll2.setBounds(590, 450, 570, 400);

    pane.add(lbl1);
    pane.add(lbl2);
    pane.add(lbl3);
    pane.add(txtfld1);
    pane.add(txtfld2);
    pane.add(txtfld3);
    pane.add(btn1);
    pane.add(btn2);
    pane.add(btn3);
    pane.add(btn4);
    pane.add(btn5);
    pane.add(btn6);
    pane.add(btn7);
    pane.add(btn8);
    pane.add(btn9);
    pane.add(btn10);
    pane.add(btn11);
    pane.add(scroll);
    pane.add(scroll1);
    pane.add(scroll2);
    pane.add(scroll3);
  }

  //handles action and all the things ^_^

  @Override
  public void actionPerformed(ActionEvent e) {
    System.out.println(e.getActionCommand());
    try {
      InfectionMessage msg;
      switch (e.getActionCommand()) {
        case "List Local Stored Block":
          txtArea1.setText(p.listStoredBlocks());
          break;
        case "BID":
          try {
            UUID id = UUID.fromString(txtfld1.getText());
            Key key = p.findOwnerOf(id);
            if (key != null) {
              p.findNode(key, "BID", id.toString());
            } else {
              JOptionPane.showMessageDialog(
                this,
                "There is no item known with this UUID"
              );
            }
          } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid UUID");
          }

          break;
        case "List Routing Table":
          txtArea1.setText(p.listRoutingTable());
          break;
        case "Block Creation":
          BlockCreation blockCreation = new BlockCreation(this.p);
          blockCreation.start();
          break;
        case "List To Sell Items":
          txtArea1.setText(p.listToSell());
          break;
        case "Advertise Items":
          java.util.List<String> toSell = new ArrayList<>();
          for (Item item : p.toSell) {
            if (!item.isAlreadyAdvertised()) {
              toSell.add(
                item.getId().toString() +
                ":" +
                item.getTitle() +
                ":" +
                item.getDescription() +
                ":" +
                item.getPrice()
              );
              item.setAlreadyAdvertised(true);
            }
          }
          msg =
            InfectionMessage
              .builder()
              .toSell(toSell)
              .seqNumber(new Random().nextLong())
              .build();
          p.infection(msg);

          break;
        case "List Intersection Int":
          txtArea1.setText(p.listIntersectionInterests());
          break;
        case "List External Int":
          txtArea1.setText(p.listExternalInterests());
          break;
        case "List Internal Int":
          txtArea1.setText(p.listInternalInterest());
          break;
        case "Submit":
          java.util.List<String> toBuy = new ArrayList<>();
          for (String str : txtfld3.getText().split(" ")) {
            toBuy.add(str);
            p.internalInterests.add(str);
          }

          msg =
            InfectionMessage
              .builder()
              .toBuy(toBuy)
              .seqNumber(new Random().nextLong())
              .build();
          p.infection(msg);

          break;
      }
      //basic error catching
    } catch (NumberFormatException ex) {
      System.out.println("Exception: " + ex);
      JOptionPane.showMessageDialog(this, "Please enter a warning message");
    } catch (ArrayIndexOutOfBoundsException ex) {
      //warnings..
    } catch (NegativeArraySizeException ex) {
      //warnings..
    }
  }

  public void log(String txt) {
    txtArea3.setText(txtArea3.getText() + "\n" + txt);
  }

  class Task1 extends TimerTask {

    public void run() {
      txtArea2.setText(p.listInternalInterest());
      txtArea4.setText(p.listFormatedInterests());
      java.util.List<Item> toRemove = new ArrayList<>();
      for (Item item : p.toSell) {
        if (
          new Timestamp(item.getEndTime())
          .before(new Timestamp(System.currentTimeMillis()))
        ) {
          toRemove.add(item);
          InfectionMessage msg = InfectionMessage
            .builder()
            .seqNumber(new Random().nextLong())
            .block(
              Block
                .builder()
                .to(item.getLastBider())
                .from(p.localNode.getId())
                .winner(null)
                .item(item)
                .previousHash(
                  p.previousBlock == null ? "init" : p.previousBlock.getHash()
                )
                .build()
            )
            .build();
          p.log(
            "Advertising block generation for item " +
            item.getTitle() +
            ":" +
            item.getId()
          );
          p.infection(msg);
        }
      }
  
      p.toSell.removeAll(toRemove);
    }
  }
}
