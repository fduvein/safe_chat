import java.util.ArrayList;

/**
 * Created by TanXin on 2016/5/21.
 */
class MessageList {
    String id = null;
    ArrayList<Message> list = new ArrayList<Message>();

    public MessageList(String id) {
        this.id = id;
    }

    private class Message {
        boolean a;//TRUE means message sent ;FALSE means message received
        String message;

        Message(boolean i, String m) {
            a = i;
            message = m;
        }
    }

    void sendMessage(String a){
        list.add(new Message(true,a));
    }
    void receiveMessage(String a) {
        list.add(new Message(false,a));
    }

    @Override
    public String toString() {
        String history="";
        String line=null;
        //U means you, H means your friend
        for(int i=0;i<list.size();i++){
            line=(list.get(i).a)?"U :":"H :";
            line+=list.get(i).message+"\n";
            history+=line;
        }
        return history;
    }
}
