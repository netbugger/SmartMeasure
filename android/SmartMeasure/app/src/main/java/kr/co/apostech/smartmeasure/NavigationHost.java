package kr.co.apostech.smartmeasure;

import androidx.fragment.app.Fragment;

interface NavigationHost {
    void navigateTo(Fragment fragment, boolean addToBackstack);
}
