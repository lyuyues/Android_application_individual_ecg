package ca.uvic.ece.ecg.heartcarer1;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import androidx.fragment.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.legacy.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity is the main Activity after logging in (or with no account)
 */
@SuppressLint("HandlerLeak")
public class MainActivity extends FragmentActivity implements HrmFragment.sendVoidToSMListener {
    private final String TAG = "MainActivity";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mTitles = new String[6];
    private int[] mIconIds = new int[6];
    private Bitmap[] mIcons = new Bitmap[6];
    private int[] menuIds = new int[6];
    private int[] menuIdsNotLogin = new int[3];
    private int curDrawerPos = -1;
    public static BluetoothAdapter mBluetoothAdapter;
    private Messenger ServiceMessenger;
    private boolean ifBackPressed = false;
    private Menu myMenu;
    private BleReceiver bleReceiver;
    private NetworkStateReceiver networkStateReceiver;
    private ArrayList<Integer> menuItemList = new ArrayList<Integer>();
    private Handler mHandler = new Handler();
    private static BaseAdapter mListAdapter;
    private ActionBar mActionBar;
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            ifBackPressed = false;
        }
    };
    // Local Messenger used to talk to ServiceMessenger, Message received by
    // IncomingHandler
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    // Location Services
    public static boolean ifLCConnected = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        Global.initiate(MainActivity.this);
        setContentView(R.layout.main_activity);
        initTitlesAndIcons();

        // Pre-fill the userName text view
        final String userName = getIntent().getStringExtra("userName");

        // Get navigation bar
        mActionBar = getActionBar();
        assert (null != mActionBar);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        // "Drawer" view to be pulled out fro left edge of the window
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Tie together the functionality of DrawerLayout and the framework ActionBar to implement the recommended design for navigation drawers.
        mDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View drawerView) {
                setTitleAndIcon(getPosition(curDrawerPos));
                for (int i = 0; i < myMenu.size(); i++)
                    myMenu.getItem(i).setVisible(true);
            }

            public void onDrawerOpened(View drawerView) {
                setTitleAndIcon(-1);
                for (int i = 0; i < myMenu.size(); i++)
                    myMenu.getItem(i).setVisible(false);
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerList = findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(mListAdapter = new MyListAdapter(this));
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> selectItem(position));

        selectItem(0);
        setTitleAndIcon(0);

        bindService(new Intent(MainActivity.this, BleService.class), mConn, Context.BIND_AUTO_CREATE);
        startService(new Intent(MainActivity.this, BleService.class));
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        assert userName != null;
        Global.toastMakeText(MainActivity.this,
                getResources().getString(R.string.main_hi) + (userName.equals("") ? "" : (" " + userName))
                        + getResources().getString(R.string.main_welcome) + getResources().getString(R.string.app_name)
                        + "!");
        Memory_Managemnt_Util memory = new Memory_Managemnt_Util();
        memory.Memory_management();

        // Bluetooth states change listener
        bleReceiver = new BleReceiver(MainActivity.this);
        IntentFilter bleFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        bleFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(bleReceiver, bleFilter);

        networkStateReceiver = new NetworkStateReceiver();
        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        networkFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        this.registerReceiver(networkStateReceiver, networkFilter);

    }

    // Connection to BleService using Messenger
    private ServiceConnection mConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            Log.i(TAG, "onServiceConnected()");
            ServiceMessenger = new Messenger(binder);
            try {
                // Register
                Message msg = Message.obtain(null, 0);
                msg.replyTo = mMessenger;
                ServiceMessenger.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Called when a connection to the Service has been lost.
        // This typically happens when the process hosting the service has
        // crashed or been killed.
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisconnected()");
        }
    };

    // Send void message to BleService
    public void sendVoidToSM(int i) {
        try {
            Message msg = Message.obtain(null, i);
            ServiceMessenger.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiate menu list's titles and icons
     */
    private void initTitlesAndIcons() {
        Resources res = MainActivity.this.getResources();

        mTitles[0] = getResources().getString(R.string.main_hrm);
        mIconIds[0] = R.drawable.hrmonitor_64;
        mIcons[0] = BitmapFactory.decodeResource(res, mIconIds[0]);

        mTitles[1] = getResources().getString(R.string.main_data);
        mIconIds[1] = R.drawable.clouddata_128;
        mIcons[1] = BitmapFactory.decodeResource(res, mIconIds[1]);

        mTitles[2] = getResources().getString(R.string.main_settings);
        mIconIds[2] = R.drawable.setting_128;
        mIcons[2] = BitmapFactory.decodeResource(res, mIconIds[2]);

        mTitles[3] = getResources().getString(R.string.main_about);
        mIconIds[3] = R.drawable.about_64;
        mIcons[3] = BitmapFactory.decodeResource(res, mIconIds[3]);

        mTitles[4] = getResources().getString(R.string.main_mydoctors);
        mIconIds[4] = R.drawable.doctor;
        mIcons[4] = BitmapFactory.decodeResource(res, mIconIds[4]);

        mTitles[5] = getResources().getString(R.string.main_exit);
        mIconIds[5] = R.drawable.exit_64;
        mIcons[5] = BitmapFactory.decodeResource(res, mIconIds[5]);

        //Logined menu
        menuIds[0] = 0;
        menuIds[1] = 1;
        menuIds[2] = 2;
        menuIds[3] = 3;
        menuIds[4] = 4;
        menuIds[5] = 5;

        //non-logined menu
        menuIdsNotLogin[0] = 0;
        menuIdsNotLogin[1] = 3;
        menuIdsNotLogin[2] = 5;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private int getPosition(int position) {
        return !Global.isLogin() ? menuIdsNotLogin[position] : menuIds[position];
    }

    /**
     * Handle drawer item clicked event
     *  Logined: 0 : monitor, 1 : about 2: exit
     *  non-login: 0 :monitor  1: Data management 2: Setting 3: about 4 my Doctors  5 exit
     * @param position:
     *            Position selected
     */
    private void selectItem(int position) {
        mDrawerLayout.closeDrawer(mDrawerList);
        if (!Global.isLogin()) {
            if (position == 2) {
                Global.exitDialog(MainActivity.this);
            } else if (position != curDrawerPos) {
                curDrawerPos = position;
                Fragment tmp = null;
                if (position == 1)
                    tmp = new AboutFragment();
                else if (position == 0)
                    tmp = new HrmFragment();

                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[getPosition(position)]).commit();
            }
        } else {
            if (position == 5) {
                mDrawerLayout.closeDrawer(mDrawerList);
                mDrawerList.setItemChecked(curDrawerPos, true);
                Global.exitDialog(MainActivity.this);
            } else if (position != curDrawerPos) {
                curDrawerPos = position;
                Fragment tmp = null;
                if (position == 0)
                    tmp = new HrmFragment();
                else if (position == 1)
                    tmp = new DataManageFragment();
                else if (position == 2)
                    tmp = new SettingFragment();
                else if (position == 3)
                    tmp = new AboutFragment();
                else if (position == 4)
                    tmp = new DoctorList();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, tmp, mTitles[getPosition(position)]).commit();
            }
        }
        mDrawerList.setItemChecked(curDrawerPos, true);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        myMenu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Customized ListAdapter for Drawer
     */
    private class MyListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        MyListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }
        public int getCount() {
            return !Global.isLogin() ? 3 : 6;
        }

        public Object getItem(int position) {
            return mTitles[getPosition(position)];
        }

        public long getItemId(int position) {
            return getPosition(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.drawer_list_item, null);
                holder = new ViewHolder();
                holder.ll = convertView.findViewById(R.id.linearLayout1);
                holder.icon = convertView.findViewById(R.id.imageView1);
                holder.title = convertView.findViewById(R.id.textView1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.ll.setBackgroundColor(Global.color_Grey);
            holder.title.setText(mTitles[getPosition(position)]);
            holder.icon.setImageBitmap(mIcons[getPosition(position)]);
            return convertView;
        }

        private class ViewHolder {
            private LinearLayout ll;
            private ImageView icon;
            private TextView title;
        }
    }

    /**
     * Update menu list view
     */
    public static void updateAdapter() {
        mListAdapter.notifyDataSetChanged();
    }

    // Set the title and icon of ActionBar
    private void setTitleAndIcon(int index) {
        if (-1 == index) {
            mActionBar.setTitle(getResources().getString(R.string.app_name_title));
            mActionBar.setIcon(R.drawable.main_heart_beat_64);
        } else {
            mActionBar.setTitle(mTitles[index]);
            mActionBar.setIcon(mIconIds[index]);
        }
    }


    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }
        if (ifBackPressed) {
            ifBackPressed = false;
            mHandler.removeCallbacks(mRunnable);
            moveTaskToBack(false);
            return;
        }
        ifBackPressed = true;
        Global.toastMakeText(MainActivity.this, getResources().getString(R.string.main_pressback));
        mHandler.postDelayed(mRunnable, Global.backInterval);
    }

    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
    }

    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        this.unregisterReceiver(bleReceiver);
        this.unregisterReceiver(networkStateReceiver);
        mDrawerLayout.removeDrawerListener(mDrawerToggle);
        Global.toastMakeText(MainActivity.this, getResources().getString(R.string.main_thank)
                + getResources().getString(R.string.app_name) + getResources().getString(R.string.excla));
        unbindService(mConn);
        stopService(new Intent(MainActivity.this, BleService.class));
        if (Global.ifTurnOffBt)
            mBluetoothAdapter.disable();
        if (ifBackPressed) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
}