// ==================== DATA MANAGEMENT CLASS ====================
class ApplicationDataManager {
    constructor() {
        this.allApplications = this.loadAllApplications();
        this.currentMonth = '';
        this.filteredApplications = [];
    }

    // Load all applications from localStorage
    loadAllApplications() {
        try {
            const apps = JSON.parse(localStorage.getItem('allApplications')) || [];
            console.log(`Loaded ${apps.length} applications from storage`);
            return apps;
        } catch (error) {
            console.error('Error loading applications:', error);
            return [];
        }
    }

    // Get applications by month (YYYY-MM format)
    getApplicationsByMonth(month) {
        this.currentMonth = month;
        
        if (!month) {
            this.filteredApplications = this.allApplications;
        } else {
            this.filteredApplications = this.allApplications.filter(app => {
                if (!app.createdAt) return false;
                const appDate = new Date(app.createdAt);
                const appMonth = appDate.toISOString().substring(0, 7); // YYYY-MM
                return appMonth === month;
            });
        }
        return this.filteredApplications;
    }

    // Search application by application number
    searchApplication(appNo) {
        if (!appNo) return this.filteredApplications;
        
        return this.filteredApplications.filter(app => 
            app.application_no && app.application_no.includes(appNo)
        );
    }

    // Get application statistics
    getStatistics() {
        const stats = {
            total: this.allApplications.length,
            byStatus: {},
            byMonth: {},
            byFeeder: {}
        };

        this.allApplications.forEach(app => {
            // Status statistics
            const status = app.status || 'unknown';
            stats.byStatus[status] = (stats.byStatus[status] || 0) + 1;

            // Month statistics
            if (app.createdAt) {
                const month = new Date(app.createdAt).toISOString().substring(0, 7);
                stats.byMonth[month] = (stats.byMonth[month] || 0) + 1;
            }

            // Feeder statistics
            const feeder = app.feeder || 'unknown';
            stats.byFeeder[feeder] = (stats.byFeeder[feeder] || 0) + 1;
        });

        return stats;
    }

    // Get complaints text from application
    getComplaintsText(application) {
        const complaints = [];
        
        // Check which complaints are selected
        const complaintLabels = [
            'লোড ছাড়াও মিটারে টাকা কাটে / টাকা কাটে না',
            'মিটার টুইস্ট সীল/লীড/প্যাডলক সীল ভাঙ্গা / চুরি',
            'মিটার ভেঙ্গে / হারিয়ে / পুড়ে গেছে',
            'মিটার স্থানান্তর / পরিবর্তন করতে আগ্রহী',
            'মিটারের ডিসপ্লেতে ডিজিট এলোমেলো',
            'মিটার কার্ড হারিয়েছে',
            'মিটারের বাটন কাজ করে না',
            'টার্মিনাল কভার খুলে গেছে',
            'মিটারে আউটপুট নেই',
            'মিটারে ডেট/টাইম সঠিক না',
            'নাম পরিবর্তন প্রয়োজন',
            'লোড বৃদ্ধি প্রয়োজন',
            'ব্যাটারি/লো-ভোল্টেজ সমস্যা',
            'মিটারে কোন পাওয়ার নেই',
            'অন্যান্য'
        ];

        complaintLabels.forEach((label, index) => {
            if (application[`complaint${index + 1}`] === true) {
                complaints.push(label);
            }
        });

        return complaints.join(', ');
    }

    // Export to Excel format
    exportToExcel() {
        if (this.filteredApplications.length === 0) {
            alert('কোন আবেদন নেই এক্সপোর্ট করার জন্য!');
            return null;
        }

        const excelData = this.filteredApplications.map(app => ({
            'আবেদন নং': app.application_no || 'N/A',
            'গ্রাহক/আবেদনকারীর নাম': app.customer_name || 'N/A',
            'পিতা/স্বামীর নাম': app.father_name || 'N/A',
            'ঠিকানা': app.address || 'N/A',
            'টেলিফোন নং': app.mobile_no || 'N/A',
            'এনআইডি নং': app.nid || 'N/A',
            'মিটার নং': app.meter_no || 'N/A',
            'গ্রাহক নং': app.consumer_no || 'N/A',
            'বকেয়া': app.arrear || 'কোন বকেয়া নেই',
            'অভিযোগ': this.getComplaintsText(app),
            'স্ট্যাটাস': this.getStatusText(app.status),
            'ফিডার': app.feeder || 'N/A',
            'তৈরির তারিখ': app.createdAt || 'N/A',
            'সর্বশেষ আপডেট': app.updatedAt || 'N/A'
        }));

        return excelData;
    }

    // Get status text in Bengali
    getStatusText(status) {
        const statusMap = {
            'created': 'নতুন আবেদন',
            'assigned_to_ae': 'AE/SDE তে Assign করা হয়েছে',
            'assigned_to_sae': 'SAE তে Assign করা হয়েছে',
            'field_survey_completed': 'ফিল্ড সার্ভে সম্পন্ন',
            'verified_by_ae': 'AE Verify করেছেন',
            'approved_by_xen': 'XEN অনুমোদন করেছেন',
            'rejected': 'প্রত্যাখ্যান করা হয়েছে',
            'action_taken': 'গৃহীত পদক্ষেপ',
            'completed': 'সম্পন্ন'
        };
        
        return statusMap[status] || status || 'স্ট্যাটাস অজানা';
    }

    // Refresh data
    refreshData() {
        this.allApplications = this.loadAllApplications();
        if (this.currentMonth) {
            this.getApplicationsByMonth(this.currentMonth);
        }
    }
}

// Create global instance
const dataManager = new ApplicationDataManager();