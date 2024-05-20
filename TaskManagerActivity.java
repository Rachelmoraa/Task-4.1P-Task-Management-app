package com.makbe.taskmanager;

import android.app.DatePickerDialog;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaskManagerActivity extends AppCompatActivity {
	private TaskAdapter taskAdapter;
	private List<Task> taskList;

	private TaskDbHelper dbHelper;

	private static final int ADD_TASK_REQUEST_CODE = 1;
	private static final int EDIT_TASK_REQUEST_CODE = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		dbHelper = new TaskDbHelper(this);

		RecyclerView recyclerView = findViewById(R.id.recycler_view_tasks);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		FloatingActionButton fabAddTask = findViewById(R.id.fab_add_task);
		fabAddTask.setOnClickListener(view -> {
			Intent intent = new Intent(this, AddEditTaskActivity.class);
			startActivityForResult(intent, ADD_TASK_REQUEST_CODE);
		});

		taskList = dbHelper.getAllTasks();
		sortTasksByDueDate();

		taskAdapter = new TaskAdapter(taskList);

		taskAdapter.setOnTaskOptionsClickListener(new TaskAdapter.OnTaskOptionsClickListener() {
			@Override
			public void onEditClick(int position) {
				Task task = taskList.get(position);
				Intent intent = new Intent(TaskManagerActivity.this, AddEditTaskActivity.class);
				intent.putExtra("isEditMode", true);
				intent.putExtra("position", position);
				intent.putExtra("taskId", task.getId());
				intent.putExtra("title", task.getTitle());
				intent.putExtra("description", task.getDescription());
				intent.putExtra("dueDate", task.getDueDate());
				startActivityForResult(intent, EDIT_TASK_REQUEST_CODE);
			}

			@Override
			public void onDeleteClick(int position) {
				Task task = taskList.get(position);
				dbHelper.deleteTask(task.getId());

				taskList.remove(position);
				taskAdapter.notifyDataSetChanged();

			}
		});

		recyclerView.setAdapter(taskAdapter);
	}

	private void sortTasksByDueDate() {
		taskList.sort(Comparator.comparing(Task::getDueDate));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == ADD_TASK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
			String title = data.getStringExtra("title");
			String description = data.getStringExtra("description");
			String dueDate = data.getStringExtra("dueDate");

			Task newTask = new Task(title, description, dueDate);
			long id = dbHelper.addTask(newTask);
			newTask.setId(id);
			taskList.add(newTask);
			sortTasksByDueDate();
			taskAdapter.notifyDataSetChanged();
		}

		if (requestCode == EDIT_TASK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
			String title = data.getStringExtra("title");
			String description = data.getStringExtra("description");
			String dueDate = data.getStringExtra("dueDate");
			long taskId = data.getLongExtra("taskId", 0);
			int position = data.getIntExtra("position", 0);

			Task updatedTask = new Task(title, description, dueDate);
			updatedTask.setId(taskId);

			int result = dbHelper.updateTask(updatedTask);

			if (result < 1) {
				Toast.makeText(this, "Couldn't update task!", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Task updated successfully", Toast.LENGTH_LONG).show();
				taskList.set(position, updatedTask);
				sortTasksByDueDate();
				taskAdapter.notifyDataSetChanged();
			}
		}
	}

}

class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
	private final List<Task> taskList;

	private OnTaskOptionsClickListener listener;

	public TaskAdapter(List<Task> taskList) {
		this.taskList = taskList;
	}

	@NonNull
	@Override
	public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
		return new TaskViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
		Task task = taskList.get(position);
		holder.bind(task);

		holder.optionsButton.setOnClickListener(view -> showOptionsPopupMenu(view, position));
	}

	@Override
	public int getItemCount() {
		return taskList.size();
	}

	public interface OnTaskOptionsClickListener {
		void onEditClick(int position);
		void onDeleteClick(int position);
	}

	public void setOnTaskOptionsClickListener(OnTaskOptionsClickListener listener) {
		this.listener = listener;
	}

	private void showOptionsPopupMenu(View view, final int position) {
		PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.inflate(R.menu.task_options_menu);
		popupMenu.setOnMenuItemClickListener(item -> {
			int id = item.getItemId();
			if(id == R.id.action_edit && listener != null) {
				listener.onEditClick(position);
				return true;
			} else if(id == R.id.action_delete && listener != null) {
				listener.onDeleteClick(position);
				return true;
			}
			return false;
		});
		popupMenu.show();
	}

	public static class TaskViewHolder extends RecyclerView.ViewHolder {
		private final TextView textViewTitle;
		private final TextView textViewDescription;
		private final TextView textViewDueDate;

		private final ImageView optionsButton;

		public TaskViewHolder(@NonNull View itemView) {
			super(itemView);
			textViewTitle = itemView.findViewById(R.id.text_view_task_title);
			textViewDescription = itemView.findViewById(R.id.text_view_task_description);
			textViewDueDate = itemView.findViewById(R.id.text_view_task_due_date);
			optionsButton = itemView.findViewById(R.id.options_button);

			itemView.setOnClickListener(view -> {
				Context context = view.getContext();
				Intent intent = new Intent(context, TaskDetailActivity.class);
				intent.putExtra("title", textViewTitle.getText().toString());
				intent.putExtra("description", textViewDescription.getText().toString());
				intent.putExtra("dueDate", textViewDueDate.getText().toString());
				context.startActivity(intent);
			});

		}

		public void bind(Task task) {
			textViewTitle.setText(task.getTitle());
			textViewDescription.setText(task.getDescription());
			textViewDueDate.setText(String.format("Due Date: %s", task.getDueDate()));
		}
	}
}


class Task {
	private long id;
	private String title;
	private String description;
	private String dueDate;

	public Task(String title, String description, String dueDate) {
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
	}

	public Task() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	@NotNull
	@Override
	public String toString() {
		return "Task {" +
				"id=" + id +
				", title='" + title + '\'' +
				", description='" + description + '\'' +
				", dueDate='" + dueDate + '\'' +
				'}';
	}
}

class TaskDbHelper extends SQLiteOpenHelper {
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "Tasks.db";

	private static final String TABLE_NAME = "tasks";
	private static final String COLUMN_ID = "id";
	private static final String COLUMN_TITLE = "title";
	private static final String COLUMN_DESCRIPTION = "description";
	private static final String COLUMN_DUE_DATE = "due_date";

	private static final String CREATE_TABLE_QUERY =
			"CREATE TABLE " + TABLE_NAME + " (" +
					COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_TITLE + " TEXT, " +
					COLUMN_DESCRIPTION + " TEXT, " +
					COLUMN_DUE_DATE + " TEXT)";

	private static final String SQL_DELETE_ENTRIES =
			"DROP TABLE IF EXISTS " + TABLE_NAME;

	public TaskDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DELETE_ENTRIES);
		onCreate(db);
	}

	public long addTask(Task task) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_TITLE, task.getTitle());
		values.put(COLUMN_DESCRIPTION, task.getDescription());
		values.put(COLUMN_DUE_DATE, task.getDueDate());
		long id = db.insert(TABLE_NAME, null, values);
		db.close();
		return id;
	}

	public List<Task> getAllTasks() {
		List<Task> taskList = new ArrayList<>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
		if (cursor.moveToFirst()) {
			do {
				Task task = new Task();
				task.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
				task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
				task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
				task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)));
				taskList.add(task);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return taskList;
	}

	public int updateTask(Task task) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_TITLE, task.getTitle());
		values.put(COLUMN_DESCRIPTION, task.getDescription());
		values.put(COLUMN_DUE_DATE, task.getDueDate());
		int rowsAffected = db.update(TABLE_NAME, values, COLUMN_ID + " = ?",
				new String[]{String.valueOf(task.getId())});
		db.close();
		return rowsAffected;
	}

	public void deleteTask(long taskId) {
		int id = (int) taskId;
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id);
		db.close();

	}
}

class TaskDetailActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_task_detail);

		Intent intent = getIntent();
		String title = intent.getStringExtra("title");
		String description = intent.getStringExtra("description");
		String dueDate = intent.getStringExtra("dueDate");

		setTitle(title);

		TextView titleTextView = findViewById(R.id.text_task_title);
		TextView descriptionTextView = findViewById(R.id.text_task_description);
		TextView dueDateTextView = findViewById(R.id.text_task_due_date);

		titleTextView.setText(title);
		descriptionTextView.setText(description);
		dueDateTextView.setText(dueDate);
	}
}

class AddEditTaskActivity extends AppCompatActivity {
	private EditText editTextTitle;
	private EditText editTextDescription;
	private EditText editTextDueDate;
	private boolean isEditMode;
	private Task taskToEdit;
	private int position;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_edit_task);

		editTextTitle = findViewById(R.id.edit_text_title);
		editTextDescription = findViewById(R.id.edit_text_description);
		editTextDueDate = findViewById(R.id.edit_text_due_date);
		Button buttonAction = findViewById(R.id.button_save);

		Intent intent = getIntent();
		if (intent.hasExtra("isEditMode")) {
			isEditMode = getIntent().getBooleanExtra("isEditMode", false);

			long taskId = getIntent().getLongExtra("taskId", 0);
			String title = getIntent().getStringExtra("title");
			String description = getIntent().getStringExtra("description");
			String dueDate = getIntent().getStringExtra("dueDate");
			position = getIntent().getIntExtra("position", 0);

			taskToEdit = new Task(title, description, dueDate);
			taskToEdit.setId(taskId);

			editTextTitle.setText(taskToEdit.getTitle());
			editTextDescription.setText(taskToEdit.getDescription());
			editTextDueDate.setText(taskToEdit.getDueDate());

			buttonAction.setText(R.string.edit);
		} else {
			buttonAction.setText(R.string.save);
		}

		buttonAction.setOnClickListener(view -> {
			if (isEditMode) {
				updateTask();
			} else {
				saveTask();
			}
		});

		editTextDueDate.setOnClickListener(view -> showDatePickerDialog());
	}

	private void showDatePickerDialog() {
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

		if (taskToEdit != null && taskToEdit.getDueDate() != null && !taskToEdit.getDueDate().isEmpty()) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
				Date date = sdf.parse(taskToEdit.getDueDate());
				calendar.setTime(date);
				year = calendar.get(Calendar.YEAR);
				month = calendar.get(Calendar.MONTH);
				dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		DatePickerDialog datePickerDialog = new DatePickerDialog(AddEditTaskActivity.this,
				(view, year1, monthOfYear, dayOfMonth1) -> {
					String selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth1);
					editTextDueDate.setText(selectedDate);
				}, year, month, dayOfMonth);
		datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

		datePickerDialog.show();
	}

	private void saveTask() {
		String title = editTextTitle.getText().toString().trim();
		String description = editTextDescription.getText().toString().trim();
		String dueDate = editTextDueDate.getText().toString().trim();

		if (validateFields()) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("title", title);
			resultIntent.putExtra("description", description);
			resultIntent.putExtra("dueDate", dueDate);
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}

	private void updateTask() {
		String title = editTextTitle.getText().toString().trim();
		String description = editTextDescription.getText().toString().trim();
		String dueDate = editTextDueDate.getText().toString().trim();

		if (validateFields()) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra("title", title);
			resultIntent.putExtra("description", description);
			resultIntent.putExtra("dueDate", dueDate);
			resultIntent.putExtra("taskId", taskToEdit.getId());
			resultIntent.putExtra("position", position);
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}

	private boolean validateFields() {
		String title = editTextTitle.getText().toString().trim();
		String description = editTextDescription.getText().toString().trim();
		String dueDate = editTextDueDate.getText().toString().trim();

		if (TextUtils.isEmpty(title)) {
			showMessage("Title is required");
			editTextTitle.setError("Title is required");
			return false;
		}

		if (TextUtils.isEmpty(description)) {
			showMessage("Description is required");
			editTextDescription.setError("Description is required");
			return false;
		}

		if (TextUtils.isEmpty(dueDate)) {
			showMessage("Due Date is required");
			editTextDueDate.setError("Due Date is required");
			return false;
		}

		return true;
	}

	private void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}

