package ca.uvic.ece.ecg.heartcarer1;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import ca.uvic.ece.ecg.ECG.DataFilter;

/**
 * This Activity plots selected ECG data
 *
 * @author yizhou
 */
public class DataManagePlot extends Activity {
    private ECGChart ecgChart1;
    private ECGChart ecgChart2;

    private DataFilter dataFilter1 = new DataFilter();
    private DataFilter dataFilter2 = new DataFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datamanage_plot);

        setTitle("History Data Plot");
        getActionBar().setIcon(R.drawable.plot_128);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        ecgChart1 = new ECGChart(DataManagePlot.this, Global.yAxis_Min_Channel1,
                Global.yAxis_Max_Channel1, getResources().getString(R.string.ecgsignal1));
        ecgChart1.addToLayout((LinearLayout) findViewById(R.id.chart1),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ecgChart2 = new ECGChart(DataManagePlot.this, Global.yAxis_Min_Channel2,
                Global.yAxis_Max_Channel2, getResources().getString(R.string.ecgsignal2));
        ecgChart2.addToLayout((LinearLayout) findViewById(R.id.chart2),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        readFromFile(getIntent().getStringExtra("fileName"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return false;
    }

    private void readFromFile(final String fileName) {
        final ProgressDialog dialog = ProgressDialog.show(DataManagePlot.this, "Drawing...", "", true, false);
        new Thread() {
            @Override
            public void run() {
                byte[] data = new byte[5];
                InputStream insputStream = null;
                try {
                    insputStream = new FileInputStream(new File(fileName));

                    while (insputStream.read(data) != -1) {
                        ecgChart1.appendPointWithoutPaint(dataFilter1.dataConvert(((data[1]) << 8) | (data[2] & 0xFF)) / 2);
                        ecgChart2.appendPointWithoutPaint(dataFilter2.dataConvert(((data[3]) << 8) | (data[4] & 0xFF)) / 2);
                    }
                } catch (Exception ignore) {
                } finally {
                    ecgChart1.repaint();
                    ecgChart2.repaint();

                    dialog.dismiss();

                    if (insputStream != null) {
                        try {
                            insputStream.close();
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }.start();
    }
}
