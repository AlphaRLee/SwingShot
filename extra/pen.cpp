#define _USE_MATH_DEFINES
#include <iostream>
#include <iomanip>
#include <math.h>

using namespace std;

int main() {
	double g = 9.81;
	double l = 1;

	double t = 0.01;
	double t_max;
	
	cout << "Pendulum length: ";
	cin >> l;

	cout << "Timestep: ";
	cin >> t;
	
	cout << "Iteration count: ";
	cin >> t_max;

	double theta = M_PI / 2, v = 0;

	cout << "t, theta, v, x, y" << endl;
	for (double i = 0; i < t_max; i++) {
		v += + t * (g / l * ((double) -1)) * sin(theta);
		theta += + t * v;
		
		cout << setw(3) << i * t << ",\t";
		cout << setw(8) << theta << ",\t";
		cout << setw(8) << v << ",\t";
		cout << setw(8) << sin(theta) << ",\t";
		cout << setw(8) << cos(theta) << endl;
		
		// v_i = v;
		// theta_i = theta;
	}
}