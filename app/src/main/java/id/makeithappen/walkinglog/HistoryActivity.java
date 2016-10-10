package id.makeithappen.walkinglog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    TextView tvEmptyHistory;
    ListView listHistory;
    List<History> list;

    Context context = this;

    DatabaseHandler db = new DatabaseHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        tvEmptyHistory = (TextView) findViewById(R.id.tvEmptyHistory);
        listHistory = (ListView) findViewById(R.id.listHistory);

        if(db.isTableEmpty()) {
            list = db.getAllHistory();
            listHistory.setAdapter(new CustomAdapter());
        }else{
            tvEmptyHistory.setText("Sorry, you doesn't have any history yet. Let's Start!");
        }
    }

    //Create Custom Adapter for our Custom List View
    public class CustomAdapter extends BaseAdapter {

        LayoutInflater mInflater;

        public CustomAdapter() {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_history,null);
            }

            TextView tvDateHistory = (TextView) convertView.findViewById(R.id.tvDateHistory);
            TextView tvStepHistory = (TextView) convertView.findViewById(R.id.tvStepHistory);
            TextView tvDurationHistory = (TextView) convertView.findViewById(R.id.tvDurationHistory);
            TextView tvDistanceHistory = (TextView) convertView.findViewById(R.id.tvDistanceHistory);
            TextView tvCalorieHistory = (TextView) convertView.findViewById(R.id.tvCalorieHistory);
            TextView tvFrequencyHistory = (TextView) convertView.findViewById(R.id.tvFrequencyHistory);
            ImageButton btnDelete = (ImageButton) convertView.findViewById(R.id.btnDelete);

            // Setting the text to display
            String historyDate = list.get(position).getDate();
            int historyStep = list.get(position).getStep();
            String historyDuration = list.get(position).getDuration();
            double historyDistance = list.get(position).getDistance();
            int historyCalorie = list.get(position).getCalorie();
            int historyFrequency = list.get(position).getFrequency();

            tvDateHistory.setText(historyDate);
            tvStepHistory.setText(String.valueOf(historyStep)+" step");
            tvDurationHistory.setText(historyDuration+"\nsec");
            tvDistanceHistory.setText(String.valueOf(historyDistance)+"\nkm");
            tvCalorieHistory.setText(String.valueOf(historyCalorie)+"\ncal");
            tvFrequencyHistory.setText(String.valueOf(historyFrequency)+"\nstep/hr");

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle("Caution")
                            .setMessage("Are you sure want to delete this record?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    db.deleteHistory(list.get(position).getID());
                                    notifyDataSetChanged();
                                    list = db.getAllHistory();
                                    listHistory.setAdapter(new CustomAdapter());
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
            });
            return convertView;
        }
    }
}
