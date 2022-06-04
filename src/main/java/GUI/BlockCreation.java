package GUI;

import blockchain.Item;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import p2p.Peer;

public class BlockCreation extends JFrame implements ActionListener {
  private static final long serialVersionUID = 1L;

  // declare some things we need
  private JLabel lbl0, lbl1, lbl2, lbl3, lbl4;
  private JTextField blockTag, blockTitle, price, duration;
  private JButton btn1;
  private JTextArea txtArea1;
  public Peer p;

  public BlockCreation(Peer p) {
    this.p = p;
  }

  public void start() {
    // make window object

    init(); // init all our things!

    // set window object size
    setSize(400, 450);
    setTitle("Block Creation");
    setVisible(true);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  public void init() {
    // create container to hold GUI in window
    Container pane = this.getContentPane();
    pane.setLayout(null);
    //0
    lbl0 = new JLabel("Tag");
    lbl0.setBounds(10, 0, 80, 20);
    blockTag = new JTextField();
    blockTag.setBounds(70, 0, 300, 20);
    //1
    lbl1 = new JLabel("Title");
    lbl1.setBounds(10, 30, 80, 20);
    blockTitle = new JTextField();
    blockTitle.setBounds(70, 30, 300, 20);
    //2
    lbl2 = new JLabel("DSC");
    lbl2.setBounds(10, 60, 80, 20);

    // 3
    lbl3 = new JLabel("price");
    lbl3.setBounds(10, 200, 80, 20);
    price = new JTextField();
    price.setBounds(70, 200, 300, 20);

    // 4
    lbl4 = new JLabel("Duration");
    lbl4.setBounds(10, 230, 80, 20);
    duration = new JTextField();
    duration.setBounds(70, 230, 300, 20);

    // generate button
    btn1 = new JButton("Create");
    btn1.setBounds(90, 330, 200, 20);
    btn1.addActionListener(this);

    txtArea1 = new JTextArea("My Text Area!");
    txtArea1.setEditable(true);
    txtArea1.setFont(new Font("monospaced", Font.PLAIN, 12));
    JScrollPane scroll = new JScrollPane(txtArea1);
    scroll.setBounds(70, 70, 300, 100);
    //add all of the things to the pane
    pane.add(lbl0);
    pane.add(lbl1);
    pane.add(lbl2);
    pane.add(lbl3);
    pane.add(lbl4);
    pane.add(blockTag);
    pane.add(blockTitle);
    pane.add(scroll);
    pane.add(price);
    pane.add(duration);
    pane.add(btn1);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    System.out.println(e.getActionCommand());
    try {
      Item item = Item
        .builder()
        .tag(blockTag.getText())
        .title(blockTitle.getText())
        .description(txtArea1.getText())
        .price(Double.parseDouble(price.getText()))
        .duration(Float.parseFloat(duration.getText()))
        .id(UUID.randomUUID())
        .creationTime(System.currentTimeMillis())
        .endTime(
          new Timestamp(
            (new Timestamp(System.currentTimeMillis())).getTime() +
            TimeUnit.MINUTES.toMillis((int) Double.parseDouble(price.getText()))
          )
          .getTime()
        )
        .build();
      p.toSell.add(item);
      
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
}
