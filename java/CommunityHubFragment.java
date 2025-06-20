package com.winlator.Download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CommunityHubFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private CommunityHubPagerAdapter pagerAdapter;

    // Tab Titles
    private static final String[] TAB_TITLES = new String[]{"Games", "Testes", "Fixes"};

    public CommunityHubFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_community_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.community_hub_view_pager);
        tabLayout = view.findViewById(R.id.community_hub_tab_layout);

        pagerAdapter = new CommunityHubPagerAdapter(requireActivity());
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    private static class CommunityHubPagerAdapter extends FragmentStateAdapter {

        public CommunityHubPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new CommunityGamesFragment();
                case 1:
                    return new CommunityTestFragment();
                case 2:
                    return new CommunityFixFragment();
                default:
                    // Should not happen
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length; // Number of sub-tabs
        }
    }
}
