package find.message.ui;

import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import message.find.R;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import find.message.model.Message;
import find.message.model.MessageBoard;

public class MessageController extends ContentObserver implements
		OnKeyListener, Runnable, Comparator<Message> {

	// query parameters
	private static final String[] COLUMNS = { "nodeid", "message", "status",
			"timestamp", "status_timestamp", "added" };
	private static final String WHERE_CLAUSE = "message !=?";
	private static final String[] WHERE_ARGS = { "" };

	private Ui ui;
	private MessageBoard mBoard;

	// executor to check messages status updates
	private ScheduledExecutorService executor; 

	public MessageController(Ui ui, MessageBoard mBoard) {
		super(null);
		this.ui = ui;
		this.mBoard = mBoard;

		this.executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this, 1, 1, TimeUnit.MINUTES);
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// If the event is a key-down event on the "enter" button
		if ((event.getAction() == KeyEvent.ACTION_DOWN)
				&& (keyCode == KeyEvent.KEYCODE_ENTER)) {

			EditText editText = (EditText) v;

			String nodeId = mBoard.getMe().getUserId();
			String text = editText.getText().toString();
			String status = mBoard.getString(R.string.message_created);
			long timestamp = System.currentTimeMillis();

			editText.setText("");
			if (text.trim().length() > 0) {

				Message message = new Message(nodeId, text, status, timestamp,
						timestamp, timestamp);

				if (mBoard.add(message)) {

					insertMessage(text); // insert to DB
					showMessages();
					return true;

				} else
					ui.showToast(mBoard.getString(R.string.message_repeated));

			} else
				ui.showToast(mBoard.getString(R.string.message_empty));
		}

		return false;
	}

	/**
	 * Insert message to database
	 * 
	 * @param text
	 *            message text
	 */
	private void insertMessage(String text) {
		ContentValues cv = new ContentValues();
		cv.put("customMessage", text);
		mBoard.getContentResolver().insert(MessageBoard.CUSTOMSEND_URI, cv);
	}

	@Override
	public void onChange(boolean selfChange) {
		this.onChange(selfChange, null);
	}

	@Override
	public void onChange(boolean selfChange, Uri uri) {
		addMessages(true);
	}

	public void addMessages(boolean last) {
		Cursor c = mBoard.getContentResolver().query(MessageBoard.SEND_URI,
				COLUMNS, WHERE_CLAUSE, WHERE_ARGS, null);

		if (checkRows(last, c))
			showMessages();

		c.close();
	}

	private boolean checkRows(boolean last, Cursor c) {
		boolean added = false;
		if (c.getCount() > 0) {

			if (last) {
				c.moveToLast();

				if (addMessage(c))
					added = true;

			} else {
				c.moveToFirst();
				do
					if (addMessage(c))
						added = true;
				while (c.moveToNext());
			}
		}
		return added;
	}

	private boolean addMessage(Cursor row) {
		String nodeId = row.getString(row.getColumnIndex(COLUMNS[0]));
		String text = row.getString(row.getColumnIndex(COLUMNS[1]));
		String status = row.getString(row.getColumnIndex(COLUMNS[2]));
		long timestamp = row.getLong(row.getColumnIndex(COLUMNS[3]));
		long statusTimestamp = row.getLong(row.getColumnIndex(COLUMNS[4]));
		long added = row.getLong(row.getColumnIndex(COLUMNS[5]));

//		Log.d(MessageBoard.TAG, nodeId + " " + text + " " + status + " "
//				+ timestamp + " " + statusTimestamp);

		Message message = new Message(nodeId, text, status, timestamp,
				statusTimestamp, added);
		
		return mBoard.add(message);
	}

	@Override
	public void run() {
		addMessages(false);
	}

	private void showMessages() {
		ui.addMessages(mBoard.getMessages(), this);
	}

	public void stop() {
		if (executor != null)
			executor.shutdown();
	}

	@Override
	public int compare(Message lhs, Message rhs) {
		if (lhs.getTimeAdded() < rhs.getTimeAdded())
			return -1;

		return 1;
	}
}
