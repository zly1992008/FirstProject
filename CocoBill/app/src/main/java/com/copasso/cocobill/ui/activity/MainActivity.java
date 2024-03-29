package com.copasso.cocobill.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bumptech.glide.Glide;
import com.copasso.cocobill.R;
import com.copasso.cocobill.base.BaseActivity;
import com.copasso.cocobill.common.Constants;
import com.copasso.cocobill.model.bean.local.BSort;
import com.copasso.cocobill.model.bean.local.NoteBean;
import com.copasso.cocobill.model.bean.remote.MyUser;
import com.copasso.cocobill.model.repository.LocalRepository;
import com.copasso.cocobill.ui.adapter.MainFragmentPagerAdapter;
import com.copasso.cocobill.ui.fragment.MonthChartFragment;
import com.copasso.cocobill.ui.fragment.MonthListFragment;
import com.copasso.cocobill.utils.DateUtils;
import com.copasso.cocobill.utils.GlideCacheUtil;
import com.copasso.cocobill.utils.ProgressUtils;
import com.copasso.cocobill.utils.SharedPUtils;
import com.copasso.cocobill.utils.ThemeManager;
import com.copasso.cocobill.utils.ToastUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.BmobUser;

/**
 * gengxin
 * Created by Zhouas666 on AndroidStudio
 * Date: 2019-01-08
 * Github: https://github.com/zas023
 *
 * 主界面activity
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static boolean IS_LOGIN = false;
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProgressUtils.dismiss();
            ToastUtils.show(MainActivity.this, "Complete");
        }
    };
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private TextView tOutcome;
    private TextView tIncome;
    private TextView tTotal;

    private View drawerHeader;
    private ImageView drawerIv;
    private TextView drawerTvAccount, drawerTvMail;

    protected static final int USERINFOACTIVITY_CODE = 0;
    protected static final int LOGINACTIVITY_CODE = 1;

    // Tab
    private FragmentManager mFragmentManager;
    private MainFragmentPagerAdapter mFragmentPagerAdapter;
    private MonthListFragment monthListFragment;
    private MonthChartFragment monthChartFragment;


    private MyUser currentUser;
    /***************************************************************************/
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        super.initData(savedInstanceState);

        //第一次进入将默认账单分类添加到数据库
        if (SharedPUtils.isFirstStart(mContext)) {
            Log.i(TAG, "第一次进入将默认账单分类添加到数据库");
            NoteBean note = new Gson().fromJson(Constants.BILL_NOTE, NoteBean.class);
            List<BSort> sorts = note.getOutSortlis();
            sorts.addAll(note.getInSortlis());
            LocalRepository.getInstance().saveBsorts(sorts);
            LocalRepository.getInstance().saveBPays(note.getPayinfo());
        }

        monthListFragment = new MonthListFragment();
        monthChartFragment = new MonthChartFragment();
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tablayout);
        viewPager = findViewById(R.id.main_viewpager);
        drawer = findViewById(R.id.main_drawer);
        navigationView = findViewById(R.id.main_nav_view);
        tOutcome = findViewById(R.id.t_outcome);
        tIncome = findViewById(R.id.t_income);
        tTotal = findViewById(R.id.t_total);

        //初始化Toolbar
        toolbar.setTitle("ExpenseTracker(Team9)");
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        drawerHeader = navigationView.inflateHeaderView(R.layout.drawer_header);
        drawerIv = drawerHeader.findViewById(R.id.drawer_iv);
        drawerTvAccount = drawerHeader.findViewById(R.id.drawer_tv_name);
        drawerTvMail = drawerHeader.findViewById(R.id.drawer_tv_mail);

        //设置头部账户
        setDrawerHeaderAccount();

        //初始化ViewPager
        mFragmentManager = getSupportFragmentManager();
        mFragmentPagerAdapter = new MainFragmentPagerAdapter(mFragmentManager);
        mFragmentPagerAdapter.addFragment(monthListFragment, "Detail");
        mFragmentPagerAdapter.addFragment(monthChartFragment, "Figure");

        monthListFragment.setMonthListListener((outcome, income, total) -> {
            tOutcome.setText(outcome);
            tIncome.setText(income);
            tTotal.setText(total);
        });

        viewPager.setAdapter(mFragmentPagerAdapter);

        //初始化TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Detail"));
        tabLayout.addTab(tabLayout.newTab().setText("Figure"));
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void initClick() {
        super.initClick();
        //监听侧滑菜单项
        navigationView.setNavigationItemSelectedListener(this);
        //监听侧滑菜单头部点击事件
        drawerHeader.setOnClickListener(v -> {
            if (currentUser==null){
                startActivityForResult(new Intent(mContext, LandActivity.class), LOGINACTIVITY_CODE);
            }else{
                startActivityForResult(new Intent(mContext, UserInfoActivity.class), USERINFOACTIVITY_CODE);
            }
        });
    }

    /***************************************************************************/
    /**
     * 设置toolbar右侧菜单
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_date:
                //时间选择器
                new TimePickerBuilder(mContext, (Date date, View v) -> {
                    monthListFragment.changeDate(DateUtils.date2Str(date, "yyyy"), DateUtils.date2Str(date, "MM"));
                    monthChartFragment.changeDate(DateUtils.date2Str(date, "yyyy"), DateUtils.date2Str(date, "MM"));
                }).setType(new boolean[]{true, true, false, false, false, false})
                        .setRangDate(null, Calendar.getInstance())
                        .isDialog(true)//是否显示为对话框样式
                        .build().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 监听左滑菜单
     * @param item
     * @return
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_sync:    //同步账单
                ProgressUtils.show(MainActivity.this, "Syncronizing data, please wait..");
                mHandler.sendEmptyMessageDelayed(0,2000);
                /*if (currentUser == null)
                    SnackbarUtils.show(mContext, "请先登陆");
                else
                    BmobRepository.getInstance().syncBill(currentUser.getObjectId());*/
                break;
            case R.id.nav_setting:
                startActivity(new Intent(mContext,SettingActivity.class));
                break;
            case R.id.nav_about:
                startActivity(new Intent(mContext,AboutActivity.class));
                break;
            case R.id.nav_theme:
                showUpdateThemeDialog();
                break;
            case R.id.nav_exit:
                exitUser();
                break;
            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    /**
     * 显示修改主题色 Dialog
     */
    private void showUpdateThemeDialog() {
        String[] themes = ThemeManager.getInstance().getThemes();
        new MaterialDialog.Builder(mContext)
                .title("Select theme")
                .titleGravity(GravityEnum.CENTER)
                .items(themes)
                .negativeText("cancel")
                .itemsCallback(((dialog, itemView, position, text) -> {
                    ThemeManager.getInstance().setTheme(mActivity, themes[position]);
                }))
                .show();
    }

    /**
     * 退出登陆 Dialog
     */
    private void exitUser(){
        new MaterialDialog.Builder(mContext)
                .title("Confirm exit")
                .content("Data will be removed")
                .positiveText("Confirm")
                .onPositive((dialog, which) -> {
                    GlideCacheUtil.getInstance().clearImageDiskCache(mContext);
                    MyUser.logOut();
                    //清除本地数据
                    LocalRepository.getInstance().deleteAllBills();
                    //重启
                    finish();
                    Intent intent = getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                })
                .negativeText("Cancel")
                .show();
    }

    /**
     * 监听Activity返回值
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case USERINFOACTIVITY_CODE:
                    setDrawerHeaderAccount();
                    break;
                case LOGINACTIVITY_CODE:
                    setDrawerHeaderAccount();
                    break;
            }
        }
    }

    /**
     * 设置DrawerHeader的用户信息
     */
    public void setDrawerHeaderAccount() {
        currentUser = BmobUser.getCurrentUser(MyUser.class);
        //获取当前用户
        if (currentUser != null) {
            drawerTvAccount.setText(currentUser.getUsername());
            drawerTvMail.setText(currentUser.getEmail());
            Glide.with(mContext).load(currentUser.getImage()).into(drawerIv);
        } else {
            drawerTvAccount.setText("Unlogin");
            drawerTvMail.setText("Click me to login");
            drawerIv.setImageResource(R.mipmap.ic_default_icon);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loginSuccess();
    }

    public void loginSuccess() {
        if (!IS_LOGIN) {
            return;
        }
        currentUser = BmobUser.getCurrentUser(MyUser.class);
        //获取当前用户
        if (currentUser != null) {
            drawerTvAccount.setText(currentUser.getUsername());
            drawerTvMail.setText(currentUser.getEmail());
            Glide.with(mContext).load(currentUser.getImage()).into(drawerIv);
        } else {
            drawerTvAccount.setText("Team9");
            drawerTvMail.setText("Welcome.");
            drawerIv.setImageResource(R.mipmap.panther);
        }
    }
}
