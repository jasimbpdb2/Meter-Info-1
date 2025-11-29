// ==================== DASHBOARD FUNCTIONS ====================

// Navigation functions
function hideAllSections() {
    const sections = document.querySelectorAll('.section');
    sections.forEach(section => section.classList.add('hidden'));
}

function showDashboard() {
    hideAllSections();
    document.getElementById('dashboardSection').classList.remove('hidden');
}

function showDataManagement() {
    hideAllSections();
    document.getElementById('dataManagementSection').classList.remove('hidden');
    loadDataManagementDashboard();
}

function openComplaintForm() {
    // Open your existing application_form.html
    if (window.AndroidInterface && AndroidInterface.openComplaintForm) {
        AndroidInterface.openComplaintForm();
    } else {
        // For web testing
        window.open('application_form.html', '_self');
    }
}

function showHistory() {
    alert('আবেদন হিস্ট্রি শীঘ্রই আসছে...');
}

function showReports() {
    alert('রিপোর্ট সিস্টেম শীঘ্রই আসছে...');
}

// ==================== DATA MANAGEMENT FUNCTIONS ====================

function loadDataManagementDashboard() {
    // Refresh data
    dataManager.refreshData();
    
    // Load default view (all applications)
    loadApplicationsByMonth();
}

function loadApplicationsByMonth() {
    const monthSelect = document.getElementById('monthSelect');
    const selectedMonth = monthSelect ? monthSelect.value : '';
    const applications = dataManager.getApplicationsByMonth(selectedMonth);
    displayApplications(applications);
}

function searchApplication() {
    const searchInput = document.getElementById('searchAppNo');
    const searchTerm = searchInput ? searchInput.value.trim() : '';
    
    let applications;
    if (searchTerm) {
        applications = dataManager.searchApplication(searchTerm);
    } else {
        applications = dataManager.filteredApplications;
    }
    
    displayApplications(applications);
}

function displayApplications(applications) {
    const applicationsList = document.getElementById('applicationsList');
    if (!applicationsList) return;

    if (applications.length === 0) {
        applicationsList.innerHTML = `
            <div style="text-align: center; padding: 20px; color: #666;">
                <h3>কোন আবেদন পাওয়া যায়নি</h3>
                <p>দয়া করে মাস নির্বাচন করুন বা সার্চ করুন</p>
            </div>
        `;
        return;
    }

    let html = `
        <div style="margin: 15px 0; font-weight: bold; color: #28a745;">
            মোট আবেদন: ${applications.length} টি
        </div>
        <div class="applications-table-container">
            <table class="applications-table">
                <thead>
                    <tr>
                        <th>আবেদন নং</th>
                        <th>গ্রাহকের নাম</th>
                        <th>মোবাইল</th>
                        <th>স্ট্যাটাস</th>
                        <th>তারিখ</th>
                        <th>একশন</th>
                    </tr>
                </thead>
                <tbody>
    `;

    applications.forEach(app => {
        html += `
            <tr>
                <td>${app.application_no || 'N/A'}</td>
                <td>${app.customer_name || 'N/A'}</td>
                <td>${app.mobile_no || 'N/A'}</td>
                <td><span class="status-badge ${getStatusClass(app.status)}">${dataManager.getStatusText(app.status)}</span></td>
                <td>${app.createdAt ? new Date(app.createdAt).toLocaleDateString('bn-BD') : 'N/A'}</td>
                <td>
                    <button class="btn" style="padding: 4px 8px; font-size: 12px;" onclick="viewApplication('${app.application_no}')">
                        দেখুন
                    </button>
                </td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    applicationsList.innerHTML = html;
}

function viewApplication(appNo) {
    const appData = dataManager.allApplications.find(app => app.application_no === appNo);
    if (appData) {
        // Open the application in view mode
        if (window.AndroidInterface && AndroidInterface.viewApplication) {
            AndroidInterface.viewApplication(JSON.stringify(appData));
        } else {
            alert('অ্যাপে আবেদন দেখানোর জন্য AndroidInterface প্রয়োজন');
        }
    } else {
        alert('আবেদন খুঁজে পাওয়া যায়নি!');
    }
}

function exportToExcel() {
    const excelData = dataManager.exportToExcel();
    if (!excelData) return;

    // For Android - send data to Java
    if (window.AndroidInterface && AndroidInterface.exportToExcel) {
        try {
            AndroidInterface.exportToExcel(JSON.stringify(excelData));
            alert('এক্সেল ফাইল তৈরি করা হয়েছে!');
        } catch (e) {
            console.error('Export failed:', e);
            alert('এক্সপোর্টে সমস্যা হয়েছে!');
        }
    } else {
        // Fallback for web
        alert(`এক্সপোর্টের জন্য ${excelData.length} টি আবেদন প্রস্তুত!`);
        console.log('Excel Data:', excelData);
    }
}

function showStats() {
    const stats = dataManager.getStatistics();
    
    alert(`
পরিসংখ্যান:
মোট আবেদন: ${stats.total} টি

স্ট্যাটাস অনুযায়ী:
${Object.entries(stats.byStatus).map(([status, count]) => 
    `${dataManager.getStatusText(status)}: ${count} টি`
).join('\n')}

মাস অনুযায়ী:
${Object.entries(stats.byMonth).map(([month, count]) => 
    `${month}: ${count} টি`
).join('\n')}
    `);
}

// Helper function for status badges
function getStatusClass(status) {
    const statusClasses = {
        'created': 'status-new',
        'assigned_to_ae': 'status-assigned',
        'assigned_to_sae': 'status-assigned',
        'field_survey_completed': 'status-in-progress',
        'verified_by_ae': 'status-verified',
        'approved_by_xen': 'status-approved',
        'rejected': 'status-rejected',
        'action_taken': 'status-action',
        'completed': 'status-completed'
    };
    return statusClasses[status] || 'status-default';
}