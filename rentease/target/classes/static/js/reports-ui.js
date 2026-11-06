(function(){
  "use strict";
  document.addEventListener("DOMContentLoaded", function(){
    const reducedMotion=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if(!reducedMotion)document.body.classList.add('ops-motion-ready');
    if(!reducedMotion&&document.body.classList.contains('review-intelligence-page'))document.body.classList.add('review-motion-ready');
    const callbacks=new WeakMap();
    const enterObserver=!reducedMotion&&'IntersectionObserver'in window?new IntersectionObserver(function(entries){
      entries.forEach(function(entry){
        if(entry.isIntersecting){
          enterObserver.unobserve(entry.target);
          const callback=callbacks.get(entry.target);
          if(callback)callback();
        }
      });
    },{threshold:.15}):null;

    function onEnter(element,callback){
      if(!element)return;
      if(!enterObserver){callback();return;}
      const r = element.getBoundingClientRect();
      const viewportHeight=window.innerHeight||document.documentElement.clientHeight;
      const visibleHeight=Math.max(0,Math.min(r.bottom,viewportHeight)-Math.max(r.top,0));
      const inView=r.top<viewportHeight&&r.bottom>0;
      const reviewSectionReady=!document.body.classList.contains('review-intelligence-page')||visibleHeight/Math.max(r.height,1)>=.15;
      if (inView&&reviewSectionReady) {
        setTimeout(callback, 60);
        return;
      }
      callbacks.set(element,callback);
      enterObserver.observe(element);
    }

    // Reviews sections reveal only when reached; each observed section is unobserved after its first entry.
    document.querySelectorAll('.review-kpi-grid,.experience-health-panel,.review-analytics-grid .review-intel-panel,.vehicle-experience-panel,.review-management-panel,.studio-report-cards,.studio-history-panel').forEach(function(section){
      onEnter(section,function(){section.classList.add('is-visible');});
    });

    // Animate the server-rendered values without changing their final formatting.
    document.querySelectorAll('.count-up').forEach(function(el){
      const format=el.dataset.format||'integer';
      const original=el.textContent.trim();
      const numericMatch=original.match(/[-+]?\d[\d.,]*/);
      const numericText=numericMatch?numericMatch[0]:'';
      const dataValue=Number(el.dataset.value);
      let raw=Number.isFinite(dataValue)?dataValue:NaN;
      if(!Number.isFinite(raw)&&numericText){
        const normalized=numericText.replace(/,(?=\d{3}(?:\D|$))/g,'');
        raw=Number(normalized);
      }
      if(!Number.isFinite(raw)||reducedMotion)return;
      const fraction=numericText.match(/[.,](\d+)$/);
      const decimals=format==='currency'?(fraction?fraction[1].length:2)
        :format==='integer'?0:(fraction?fraction[1].length:0);
      const prefix=numericMatch?original.slice(0,numericMatch.index):'';
      const suffix=numericMatch?original.slice(numericMatch.index+numericText.length):'';
      const separatorSample=numericText;
      const lastDot=separatorSample.lastIndexOf('.');
      const lastComma=separatorSample.lastIndexOf(',');
      let decimalSeparator='.';
      if(decimals>0){
        if(lastComma>lastDot&&separatorSample.length-lastComma-1===decimals)decimalSeparator=',';
        else if(lastDot>=0&&separatorSample.length-lastDot-1===decimals)decimalSeparator='.';
      }
      let groupSeparator='';
      if(lastDot>=0&&lastComma>=0)groupSeparator=decimalSeparator===','?'.':',';
      else if(lastDot>=0&&(decimals===0||decimalSeparator!=='.'))groupSeparator='.';
      else if(lastComma>=0&&(decimals===0||decimalSeparator!==','))groupSeparator=',';
      function formattedNumber(value){
        const fixed=Math.abs(value).toFixed(decimals).split('.');
        const grouped=groupSeparator?fixed[0].replace(/\B(?=(\d{3})+(?!\d))/g,groupSeparator):fixed[0];
        return (value<0?'-':'')+grouped+(decimals>0?decimalSeparator+fixed[1]:'');
      }
      el.textContent=prefix+formattedNumber(0)+suffix;
      onEnter(el,function(){
        let start=null;const duration=1280;
        function paint(ts){
          if(!start)start=ts;
          const progress=Math.min((ts-start)/duration,1);
          const eased=1-Math.pow(1-progress,3);
          const value=raw*eased;
          const display=formattedNumber(format==='integer'?Math.round(value):value);
          el.textContent=progress===1?original:prefix+display+suffix;
          if(progress<1)requestAnimationFrame(paint);
        }
        requestAnimationFrame(paint);
      });
    });

    // Progress indicators keep their true final widths and animate once when first seen.
    document.querySelectorAll('.rating-distribution,.rating-bars,.vehicle-experience-list').forEach(function(group){
      group.querySelectorAll('.meter-fill,.rating-fill').forEach(function(el,index){
        const ratingLabel=el.closest('.rating-distribution-row')?.querySelector('.rating-distribution-label')?.textContent||'';
        const ratingValue=Number((ratingLabel.match(/\d+/)||[])[0]);
        const delay=Number.isFinite(ratingValue)&&ratingValue>=1&&ratingValue<=5?(5-ratingValue)*100:index*90;
        el.style.transitionDelay=Math.min(delay,400)+'ms';
      });
    });
    document.querySelectorAll('.meter-fill,.rating-fill').forEach(function(el){
      const finalWidth=el.style.width;
      if(reducedMotion)return;
      el.style.width='0%';
      // Observe the stable track rather than a fill whose width is now zero.
      const trigger=document.body.classList.contains('review-intelligence-page')&&el.matches('.rating-fill,.vehicle-experience-meter>span')
        ?el.closest('.rating-track,.vehicle-experience-meter')||el:el;
      onEnter(trigger,function(){
        requestAnimationFrame(function(){el.style.width=finalWidth;});
      });
    });

    // Animate the CSS conic-gradient health ring from zero to its server-rendered score.
    document.querySelectorAll('.experience-health-score').forEach(function(ring){
      const scoreText=ring.style.getPropertyValue('--health-score').trim();
      const finalScore=parseFloat(scoreText);
      if(!Number.isFinite(finalScore))return;
      if(reducedMotion){
        ring.style.setProperty('--health-score',Math.max(0,Math.min(100,finalScore))+'%');
        return;
      }
      const target=Math.max(0,Math.min(100,finalScore));
      ring.style.setProperty('--health-score','0%');
      onEnter(ring,function(){
        let start=null;
        const duration=1050;
        function paint(timestamp){
          if(start===null)start=timestamp;
          const progress=Math.min((timestamp-start)/duration,1);
          const eased=1-Math.pow(1-progress,3);
          ring.style.setProperty('--health-score',(target*eased).toFixed(2)+'%');
          if(progress<1)requestAnimationFrame(paint);
        }
        requestAnimationFrame(paint);
      });
    });

    document.querySelectorAll('.performer-list').forEach(function(list){
      const rows=[...list.querySelectorAll('.performer-row')];
      rows.forEach(function(row,index){
        const fill=row.querySelector('.rank-fill');
        const image=row.querySelector('.performer-image');
        if(image&&!reducedMotion)image.style.transitionDelay=(index*65)+'ms';
        if(!fill)return;
        fill.dataset.finalWidth=fill.style.width;
        if(!reducedMotion){fill.style.width='0%';fill.style.transitionDelay=(index*75)+'ms';}
      });
      onEnter(list,function(){
        list.classList.add('is-visible');
        rows.forEach(function(row){
          const fill=row.querySelector('.rank-fill');
          if(fill&&!reducedMotion)requestAnimationFrame(function(){fill.style.width=fill.dataset.finalWidth;});
        });
      });
    });

    // Draw the revenue chart only from the real monthly payment totals in the page model.
    const chart=document.querySelector('[data-revenue-chart]');
    if(chart){
      const labels=[...chart.querySelectorAll('[data-revenue-labels] span')].map(function(node){
        return {label:node.dataset.label||'',month:node.dataset.month||'',value:Number(node.dataset.value)||0};
      });
      const empty=document.querySelector('[data-revenue-empty]');
      if(!labels.some(function(point){return point.value>0;})){
        if(empty) empty.hidden=false;
      }else{
        const svg=chart.querySelector('svg');
        const line=svg.querySelector('[data-revenue-line]');
        const area=svg.querySelector('[data-revenue-area]');
        const pointLayer=svg.querySelector('[data-revenue-points]');
        const tooltip=chart.querySelector('[data-chart-tooltip]');
        const width=720, left=44, right=710, top=28, bottom=187;
        const maximum=Math.max.apply(null,labels.map(function(point){return point.value;}))||1;
        const points=labels.map(function(point,index){
          const x=labels.length===1?(left+right)/2:left+(right-left)*index/(labels.length-1);
          const y=bottom-(bottom-top)*point.value/maximum;
          return {x:x,y:y,meta:point};
        });
        const path=points.map(function(point,index){return (index?'L':'M')+point.x.toFixed(1)+' '+point.y.toFixed(1);}).join(' ');
        line.setAttribute('d',path);
        area.setAttribute('d',path+' L '+points[points.length-1].x+' '+bottom+' L '+points[0].x+' '+bottom+' Z');
        let revealClip=svg.querySelector('#revenueRevealClip');
        if(!revealClip){
          const defs=svg.querySelector('defs')||svg.insertBefore(document.createElementNS('http://www.w3.org/2000/svg','defs'),svg.firstChild);
          revealClip=document.createElementNS('http://www.w3.org/2000/svg','clipPath');revealClip.setAttribute('id','revenueRevealClip');revealClip.setAttribute('clipPathUnits','userSpaceOnUse');
          const clipRect=document.createElementNS('http://www.w3.org/2000/svg','rect');clipRect.setAttribute('class','revenue-reveal-clip');clipRect.setAttribute('x',left);clipRect.setAttribute('y','15');clipRect.setAttribute('width',reducedMotion?right-left:0);clipRect.style.width=reducedMotion?(right-left)+'px':'0px';clipRect.setAttribute('height','185');revealClip.appendChild(clipRect);defs.appendChild(revealClip);
        }
        area.setAttribute('clip-path','url(#revenueRevealClip)');
        points.forEach(function(point,index){
          const circle=document.createElementNS('http://www.w3.org/2000/svg','circle');
          circle.setAttribute('class','revenue-point'+(point.meta.month===chart.querySelector('.revenue-labels .is-selected')?.dataset.month?' is-selected':''));circle.setAttribute('cx',point.x);circle.setAttribute('cy',point.y);circle.setAttribute('r','3.5');
          circle.style.setProperty('--revenue-point-delay',((index/Math.max(points.length-1,1))*1.1+.06).toFixed(2)+'s');
          circle.setAttribute('tabindex','0');circle.setAttribute('role','img');circle.setAttribute('aria-label',point.meta.label+' '+point.meta.month+', LKR '+point.meta.value.toFixed(2));
          const title=document.createElementNS('http://www.w3.org/2000/svg','title');
          title.textContent=point.meta.label+' '+point.meta.month+': LKR '+point.meta.value.toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2});circle.appendChild(title);pointLayer.appendChild(circle);
          const showTooltip=function(event){
            if(!tooltip)return;const bounds=chart.getBoundingClientRect();const clientX=event.clientX||bounds.left+(point.x/width)*bounds.width;
            tooltip.textContent=point.meta.label+' '+point.meta.month+' · LKR '+point.meta.value.toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2});
            tooltip.style.display='block';tooltip.style.left=Math.max(4,Math.min(bounds.width-170,clientX-bounds.left-55))+'px';tooltip.style.top=Math.max(0,(point.y/230)*bounds.height-29)+'px';
          };
          circle.addEventListener('mouseenter',showTooltip);circle.addEventListener('focus',showTooltip);circle.addEventListener('mouseleave',function(){if(tooltip)tooltip.style.display='none';});
        });
        const drawLine=function(){
          line.classList.add('is-drawn');area.classList.add('is-drawn');
          chart.classList.add('is-visible');
          const clipRect=revealClip.querySelector('rect');
          if(reducedMotion){clipRect.setAttribute('width',right-left);clipRect.style.width=(right-left)+'px';return;}
          if(line.getTotalLength){const length=line.getTotalLength();line.style.strokeDasharray=String(length);line.style.strokeDashoffset=String(length);requestAnimationFrame(function(){line.style.strokeDashoffset='0';clipRect.setAttribute('width',right-left);clipRect.style.width=(right-left)+'px';});}
        };
        onEnter(chart,drawLine);
      }
    }

    // One-time viewport reveals for the remaining operational visualizations.
    document.querySelectorAll('.fleet-gauge').forEach(function(gauge){
      const total=Number(gauge.dataset.total)||0;const circles=[...gauge.querySelectorAll('.gauge-segment')];const circumference=2*Math.PI*48;let offset=0;
      circles.forEach(function(circle){const count=Number(circle.dataset.count)||0;const length=total?circumference*count/total:0;circle.dataset.finalDash=length+' '+(circumference-length);circle.dataset.finalOffset=String(-offset);if(!reducedMotion){circle.style.strokeDasharray='0 '+circumference;circle.style.strokeDashoffset=String(-offset);}offset+=length;});
      onEnter(gauge,function(){if(reducedMotion)return;requestAnimationFrame(function(){circles.forEach(function(circle){circle.style.strokeDasharray=circle.dataset.finalDash;circle.style.strokeDashoffset=circle.dataset.finalOffset;});});});
    });
    document.querySelectorAll('.reservation-chart').forEach(function(element){onEnter(element,function(){element.classList.add('is-visible');});});
    document.querySelectorAll('.activity-list').forEach(function(element){onEnter(element,function(){element.classList.add('is-visible');});});
    document.querySelectorAll('.lifecycle-flow').forEach(function(element){onEnter(element,function(){element.classList.add('is-visible');});});

    // Draw the review-rating history from the server-rendered monthly averages.
    document.querySelectorAll('[data-review-trend]').forEach(function(chart){
      const line=chart.querySelector('[data-trend-line]');
      if(!line||reducedMotion||!line.getTotalLength)return;
      const length=line.getTotalLength();
      line.style.strokeDasharray=String(length);
      line.style.strokeDashoffset=String(length);
      const points=[...chart.querySelectorAll('.review-trend-points circle')];
      points.forEach(function(point,index){
        const delay=(index/Math.max(points.length-1,1))*.9+.06;
        point.style.setProperty('--review-point-delay',delay.toFixed(2)+'s');
      });
      onEnter(chart,function(){
        requestAnimationFrame(function(){
          line.style.strokeDashoffset='0';
          chart.classList.add('is-visible');
        });
      });
    });

    // One-time staggered reveals for the review-backed vehicle rankings and feedback rows.
    const vehicleExperienceList=document.querySelector('.vehicle-experience-list');
    if(vehicleExperienceList){
      vehicleExperienceList.querySelectorAll('.vehicle-experience-row').forEach(function(row,index){
        row.style.setProperty('--review-delay',Math.min(index*90,450)+'ms');
      });
      onEnter(vehicleExperienceList,function(){vehicleExperienceList.classList.add('is-visible');});
    }
    const studioReportCards=document.querySelector('.studio-report-cards');
    if(studioReportCards){
      studioReportCards.querySelectorAll('.studio-report-card').forEach(function(card,index){
        card.style.setProperty('--report-card-order',String(index));
      });
    }
    const reviewTableBody=document.getElementById('review-table-body');
    if(reviewTableBody){
      reviewTableBody.querySelectorAll('.review-row').forEach(function(row,index){
        row.style.setProperty('--review-delay',Math.min(index*80,400)+'ms');
      });
      onEnter(reviewTableBody,function(){reviewTableBody.classList.add('is-visible');});
    }

    // Review search/filter.
    const search=document.getElementById('review-search');
    const rating=document.getElementById('rating-filter');
    const vehicle=document.getElementById('vehicle-filter');
    const status=document.getElementById('status-filter');
    const managementStatus=document.getElementById('management-status-filter');
    const dateFilter=document.getElementById('date-filter');
    const applyFilters=document.getElementById('review-filter-apply');
    const resetFilters=document.getElementById('review-filter-reset');
    const reviewList=document.getElementById('review-table-body');
    const rows=reviewList?[...reviewList.querySelectorAll('.review-row')]:[];
    const resultCount=document.getElementById('review-count');
    const resultCountLabel=document.getElementById('review-count-label');
    const empty=document.getElementById('no-filter-results');
    const quickQueueButtons=[...document.querySelectorAll('[data-review-quick-filter]')];
    let activeQuickQueue='all';
    function filterRows(){
      const q=(search?.value||'').trim().toLocaleLowerCase();
      const selectedRating=rating?.value||'all';
      const selectedVehicle=vehicle?.value||'all';
      const selectedHealth=status?.value||'all';
      const selectedManagementStatus=managementStatus?.value||'all';
      const selectedDate=dateFilter?.value||'all';
      let visible=0;
      rows.forEach(function(row){
        const data=row.dataset;
        const searchable=[data.customerName,data.customerEmail,data.vehicle,data.reservationId,
          data.pickupLocation,data.returnLocation,data.comment,data.managementStatus,data.managementNote,data.search].join(' ').toLocaleLowerCase();
        const matchText=!q||searchable.includes(q);
        const matchRating=selectedRating==='all'||data.rating===selectedRating;
        const matchVehicle=selectedVehicle==='all'||data.vehicleId===selectedVehicle;
        const matchHealth=selectedHealth==='all'||data.status===selectedHealth;
        const currentManagementStatus=data.managementStatus||'OPEN';
        const matchManagementStatus=selectedManagementStatus==='all'
          ||(selectedManagementStatus==='unresolved'&&currentManagementStatus!=='RESOLVED')
          ||currentManagementStatus===selectedManagementStatus;
        const matchQuickQueue=activeQuickQueue==='all'
          ||(activeQuickQueue==='attention'&&data.status==='attention')
          ||(activeQuickQueue==='unresolved'&&currentManagementStatus!=='RESOLVED');
        let matchDate=selectedDate==='all';
        if(!matchDate&&data.date){
          const parts=data.date.split('-').map(Number);
          const submitted=parts.length===3?new Date(parts[0],parts[1]-1,parts[2]):new Date(NaN);
          const today=new Date();
          today.setHours(0,0,0,0);
          const ageDays=Math.floor((today.getTime()-submitted.getTime())/86400000);
          matchDate=!Number.isNaN(submitted.getTime())&&ageDays>=0&&ageDays<=Number(selectedDate);
        }
        const show=matchText&&matchRating&&matchVehicle&&matchHealth&&matchManagementStatus&&matchDate&&matchQuickQueue;
        const wasHidden=row.style.display==='none';
        row.style.display=show?'':'none';
        const revealIndex=visible;
        if(show) visible++;
        if(show&&wasHidden&&!reducedMotion&&typeof row.animate==='function')row.animate([
          {opacity:0,transform:'translateY(5px) scale(.99)'},
          {opacity:1,transform:'translateY(0) scale(1)'}
        ],{duration:360,delay:Math.min(revealIndex*80,400),easing:'cubic-bezier(.22,.8,.24,1)'});
      });
      if(resultCount) resultCount.textContent=String(visible);
      if(resultCountLabel) resultCountLabel.textContent=visible===1?'matching review':'matching reviews';
      if(empty) empty.hidden=rows.length===0||visible!==0;
    }
    function setQuickQueue(queue){
      activeQuickQueue=queue||'all';
      quickQueueButtons.forEach(function(button){
        const active=button.dataset.reviewQuickFilter===activeQuickQueue;
        button.classList.toggle('is-active',active);
        button.setAttribute('aria-pressed',String(active));
      });
      filterRows();
    }
    quickQueueButtons.forEach(function(button){
      button.addEventListener('click',function(){
        // A quick queue starts from a clean filter state so it never silently
        // combines with an older search or vehicle selection.
        if(search)search.value='';
        [rating,vehicle,status,managementStatus,dateFilter].forEach(function(el){if(el)el.value='all';});
        setQuickQueue(button.dataset.reviewQuickFilter);
      });
    });
    function clearQuickQueueForManualFilter(){
      activeQuickQueue='all';
      quickQueueButtons.forEach(function(button){
        const active=button.dataset.reviewQuickFilter==='all';
        button.classList.toggle('is-active',active);
        button.setAttribute('aria-pressed',String(active));
      });
      filterRows();
    }
    [search,rating,vehicle,status,managementStatus,dateFilter].forEach(function(el){
      if(el)el.addEventListener(el.tagName==='INPUT'?'input':'change',clearQuickQueueForManualFilter);
    });
    if(applyFilters)applyFilters.addEventListener('click',filterRows);
    if(search)search.addEventListener('keydown',function(event){
      if(event.key==='Enter'){event.preventDefault();filterRows();}
    });
    filterRows();
    function resetReviewFilters(){
      if(search)search.value='';
      [rating,vehicle,status,managementStatus,dateFilter].forEach(function(el){if(el)el.value='all';});
      setQuickQueue('all');
      if(search)search.focus();
    }
    if(resetFilters)resetFilters.addEventListener('click',resetReviewFilters);
    document.querySelectorAll('[data-review-reset]').forEach(function(button){
      button.addEventListener('click',resetReviewFilters);
    });
    document.querySelectorAll('[data-select-review-vehicle]').forEach(function(control){
      control.addEventListener('click',function(){
        if(vehicle){vehicle.value=control.dataset.selectReviewVehicle||'all';filterRows();}
        document.getElementById('review-management-title')?.scrollIntoView({behavior:reducedMotion?'auto':'smooth',block:'start'});
      });
    });
    document.querySelectorAll('[data-select-review-rating]').forEach(function(control){
      control.addEventListener('click',function(){
        if(rating){rating.value=control.dataset.selectReviewRating||'all';filterRows();}
        document.getElementById('review-management-title')?.scrollIntoView({behavior:reducedMotion?'auto':'smooth',block:'start'});
      });
    });

    // Async report generation flow for print-ready PDFs and spreadsheet-friendly CSV exports.
    const exportForm=document.getElementById('review-export-form');
    if(exportForm){
      const stepper=document.getElementById('report-generation-stepper');
      const readyCard=document.getElementById('report-ready-card');
      const errorCard=document.getElementById('report-error-card');
      const submitBtn=document.getElementById('review-export-submit');
      const submitLabel=exportForm.querySelector('[data-submit-label]');
      const formatSelect=exportForm.querySelector('[name="format"]');
      const viewLinkInitial=document.getElementById('report-view-link');
      const downloadLinkInitial=document.getElementById('report-download-link');
      function selectedFormat(){return (formatSelect?.value||'PDF').toUpperCase();}
      function updateFormatLabels(){
        const format=selectedFormat();
        if(submitLabel)submitLabel.textContent='Generate '+format;
        if(viewLinkInitial)viewLinkInitial.hidden=format!=='PDF';
        if(downloadLinkInitial)downloadLinkInitial.textContent='Download '+format+' ↓';
      }
      formatSelect?.addEventListener('change',updateFormatLabels);
      updateFormatLabels();
      const historyList=document.getElementById('report-history-list');
      const historyCount=document.getElementById('export-history-count');
      const historyEmpty=document.getElementById('report-history-empty');
      const historySection=document.getElementById('review-report-history');

      function setStep(stepNum){
        if(!stepper)return;
        const steps=stepper.querySelectorAll('.stepper-steps li');
        steps.forEach(function(li,idx){
          const num=idx+1;
          const ind=li.querySelector('.step-indicator');
          if(num<stepNum){
            li.className='is-done';
            if(ind)ind.textContent='✓';
          }else if(num===stepNum){
            li.className='is-active';
            if(ind)ind.textContent='●';
          }else{
            li.className='';
            if(ind)ind.textContent='○';
          }
        });
      }

      function formatPeriod(period){
        if(!period)return '';
        const parts=period.split('-');
        if(parts.length<2)return period;
        const d=new Date(Number(parts[0]),Number(parts[1])-1,1);
        if(Number.isNaN(d.getTime()))return period;
        return d.toLocaleDateString('en-US',{month:'long',year:'numeric'});
      }

      const reportTypeTitles={
        'RATING_ANALYSIS':'Customer feedback & rating analysis',
        'MONTHLY_REVENUE':'Monthly revenue',
        'VEHICLE_UTILIZATION':'Vehicle utilization',
        'MOST_RENTED_VEHICLES':'Most-rented vehicles'
      };

      // Preview values are server-provided operational metrics; revenue follows the selected month via its existing API.
      const preview=document.getElementById('report-preview');
      const previewType=document.getElementById('report-preview-type');
      const previewValue=document.getElementById('report-preview-value');
      const previewDetail=document.getElementById('report-preview-detail');
      const typeSelect=document.getElementById('report-type-select');
      const periodSelect=exportForm.querySelector('[name="periodLabel"]');
      const periodLabel=document.getElementById('report-period-label');
      const periodHelp=document.getElementById('report-period-help');
      const formatHelp=document.getElementById('report-format-help');
      const previewGuidance=document.getElementById('report-preview-guidance');
      const reportGuidance={
        MONTHLY_REVENUE:{periodLabel:'Reporting month',periodHelp:'Revenue and dated transactions are calculated for this calendar month.',guidance:'Use PDF for an executive-ready revenue and reconciliation report, or CSV to review payment rows in a spreadsheet.'},
        VEHICLE_UTILIZATION:{periodLabel:'Reference month',periodHelp:'Utilization is a current fleet snapshot; this month is retained as the report reference period.',guidance:'Use PDF to share the fleet snapshot, or CSV when you need to sort and compare the reported vehicle data.'},
        MOST_RENTED_VEHICLES:{periodLabel:'Reference month',periodHelp:'The ranking uses recorded reservation history, not only this month. The month labels the export.',guidance:'Use PDF for a ranked summary or CSV to work with the vehicle ranking data in a spreadsheet.'},
        RATING_ANALYSIS:{periodLabel:'Reference month',periodHelp:'Rating analysis uses saved customer reviews across available history; the month labels the export.',guidance:'Use PDF to present customer-experience findings or CSV to inspect rating data in a spreadsheet.'}
      };
      function updateReportGuidance(){
        const type=typeSelect?.value||'MONTHLY_REVENUE';
        const copy=reportGuidance[type]||reportGuidance.MONTHLY_REVENUE;
        if(periodLabel)periodLabel.textContent=copy.periodLabel;
        if(periodHelp)periodHelp.textContent=copy.periodHelp;
        if(previewGuidance)previewGuidance.textContent=copy.guidance;
        if(formatHelp&&formatSelect)formatHelp.textContent=formatSelect.value==='CSV'
          ?'CSV downloads as a spreadsheet-friendly file for sorting, filtering and further analysis.'
          :'PDF keeps the branded report layout for printing, sharing and business review.';
      }
      let previewRevision=0;
      function updateReportPreview(){
        if(!preview||!typeSelect||!previewValue)return;
        const type=typeSelect.value;
        const period=periodSelect?.value||'';
        const values={
          MONTHLY_REVENUE:{label:'MONTHLY REVENUE',value:'LKR '+Number(preview.dataset.monthlyRevenue||0).toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2}),detail:'Actual successful payments · '+period},
          VEHICLE_UTILIZATION:{label:'VEHICLE UTILIZATION',value:Number(preview.dataset.utilization||0).toFixed(1)+'%',detail:'Current fleet snapshot · pending or confirmed reservations'},
          MOST_RENTED_VEHICLES:{label:'MOST RENTED VEHICLES',value:Number(preview.dataset.rankedVehicles||0)+' vehicles ranked',detail:'Top five · all recorded reservation history'},
          RATING_ANALYSIS:{label:'RATING ANALYSIS',value:Number(preview.dataset.averageRating||0).toFixed(1)+' / 5',detail:Number(preview.dataset.reviewCount||0)+' submitted reviews across available history'}
        };
        const selected=values[type]||values.MONTHLY_REVENUE;
        const revision=++previewRevision;
        const paint=function(){
          if(revision!==previewRevision)return;
          previewType.textContent=selected.label;
          previewValue.textContent=selected.value;
          previewDetail.textContent=selected.detail;
          preview.classList.remove('is-changing');
        };
        if(!reducedMotion)preview.classList.add('is-changing');
        const schedulePaint=function(){if(reducedMotion)paint();else window.setTimeout(paint,140);};
        if(type==='MONTHLY_REVENUE'&&period&&/^\d{4}-\d{2}$/.test(period)&&preview.dataset.revenueEndpoint){
          const endpoint=new URL(preview.dataset.revenueEndpoint,window.location.href);
          endpoint.searchParams.set('month',period);
          fetch(endpoint,{headers:{'Accept':'application/json'}}).then(function(response){if(!response.ok)throw new Error('Preview unavailable');return response.json();}).then(function(amount){
            if(revision!==previewRevision)return;
            selected.value='LKR '+Number(amount||0).toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});
            selected.detail='Actual successful payments · '+period;
            schedulePaint();
          }).catch(schedulePaint);
        }else schedulePaint();
      }
      if(preview&&typeSelect){
        typeSelect.addEventListener('change',function(){updateReportGuidance();updateReportPreview();});
        periodSelect?.addEventListener('change',updateReportPreview);
        formatSelect?.addEventListener('change',updateReportGuidance);
        updateReportGuidance();
        updateReportPreview();
      }
      document.querySelectorAll('[data-generate-report]').forEach(function(button){
        button.addEventListener('click',function(){
          if(!typeSelect)return;
          typeSelect.value=button.dataset.generateReport;
          typeSelect.dispatchEvent(new Event('change',{bubbles:true}));
          exportForm.requestSubmit();
        });
      });

      function escapeHtml(str){
        return (str||'').replace(/[&<>"']/g,function(m){
          return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m];
        });
      }

      exportForm.addEventListener('submit',async function(e){
        const periodInput=exportForm.querySelector('[name="periodLabel"]');
        const staffInput=exportForm.querySelector('[name="staffId"]');
        if(!periodInput?.value||!staffInput?.value)return;
        if(!/^\d{4}-\d{2}$/.test(periodInput.value.trim()))return;

        e.preventDefault();

        if(submitBtn){submitBtn.disabled=true;submitBtn.setAttribute('aria-busy','true');}
        const requestedFormat=selectedFormat();
        if(submitLabel)submitLabel.textContent='Generating…';
        if(readyCard)readyCard.hidden=true;
        if(errorCard)errorCard.hidden=true;
        if(stepper){
          stepper.hidden=false;
          setStep(1);
          stepper.scrollIntoView({behavior:'smooth',block:'nearest'});
        }

        const formData=new FormData(exportForm);
        const actionUrl=exportForm.action||'/reports/export';
        const delay=ms=>new Promise(res=>setTimeout(res,ms));

        try{
          const fetchPromise=fetch(actionUrl,{
            method:'POST',
            body:formData,
            headers:{'Accept':'application/json'}
          });

          await delay(450);
          setStep(2);

          await delay(500);
          setStep(3);

          await delay(500);
          setStep(4);

          const [res]=await Promise.all([fetchPromise,delay(400)]);

          if(!res.ok){
            const errJson=await res.json().catch(()=>({}));
            throw new Error(errJson.message||'Server returned status '+res.status);
          }

          const data=await res.json();
          // Validate the actual selected export endpoint before presenting it as ready.
          const isPdf=requestedFormat==='PDF';
          const validationUrl=isPdf?data.viewUrl:data.downloadUrl;
          const exportResponse=await fetch(validationUrl,{headers:{'Accept':isPdf?'application/pdf':'text/csv'}});
          if(!exportResponse.ok){
            const responseType=exportResponse.headers.get('content-type')||'';
            let serverMessage='';
            if(responseType.includes('application/json')){
              const body=await exportResponse.json().catch(function(){return {};});
              serverMessage=body.message||body.error||'';
            }
            throw new Error(serverMessage||'The server could not generate this '+requestedFormat+' export (HTTP '+exportResponse.status+'). Check the application logs for details.');
          }
          const returnedType=(exportResponse.headers.get('content-type')||'').toLowerCase();
          if(isPdf&&!returnedType.includes('application/pdf'))throw new Error('The server did not return a PDF file.');
          if(!isPdf&&!returnedType.includes('text/csv'))throw new Error('The server did not return a CSV file.');
          if(exportResponse.body)await exportResponse.body.cancel();
          if(stepper)stepper.hidden=true;

          if(readyCard){
            const titleEl=document.getElementById('report-ready-title');
            const periodEl=document.getElementById('report-ready-period');
            const viewLink=document.getElementById('report-view-link');
            const downloadLink=document.getElementById('report-download-link');

            const readableType=reportTypeTitles[data.reportType]||data.reportType;
            if(titleEl)titleEl.textContent=readableType;
            if(periodEl)periodEl.textContent=formatPeriod(data.periodLabel);
            if(viewLink){viewLink.href=data.viewUrl||'#';viewLink.hidden=!isPdf;viewLink.textContent='View PDF ↗';}
            if(downloadLink){downloadLink.href=data.downloadUrl;downloadLink.textContent='Download '+requestedFormat+' ↓';}

            readyCard.hidden=false;
            readyCard.scrollIntoView({behavior:'smooth',block:'nearest'});
          }

          if(historyList){
            if(historyEmpty)historyEmpty.remove();
            if(historySection)historySection.hidden=false;

            const readableType=reportTypeTitles[data.reportType]||data.reportType;
            const article=document.createElement('article');
            article.className='review-export-row';
            article.style.animation='review-center-enter .5s cubic-bezier(.22,.8,.24,1) both';
            article.innerHTML=`
              <div class="review-export-file-mark">${escapeHtml(data.format||'PDF')}</div>
              <div>
                <strong>${escapeHtml(readableType)}</strong>
                <small>
                  <span>${escapeHtml(data.periodLabel)}</span>
                  <span>·</span>
                  <time>${escapeHtml(data.generatedAt||'Just now')}</time>
                  <span>· ${escapeHtml(data.format||'PDF')}</span>
                </small>
              </div>
              <div class="review-export-actions">
                ${isPdf?'<a class="btn btn-secondary btn-sm" target="_blank" href="'+escapeHtml(data.viewUrl)+'">View PDF&nbsp; ↗</a>':''}
                <a class="btn btn-primary btn-sm" href="${escapeHtml(data.downloadUrl)}">Download ${escapeHtml(requestedFormat)}&nbsp; ↓</a>
              </div>
            `;
            historyList.insertBefore(article,historyList.firstChild);

            if(historyCount){
              const count=historyList.querySelectorAll('.review-export-row').length;
              historyCount.textContent=count+' saved exports';
            }
          }
        }catch(err){
          if(stepper)stepper.hidden=true;
          if(errorCard){
            const msgEl=document.getElementById('report-error-message');
            if(msgEl)msgEl.textContent=err.message||'An error occurred while generating the report.';
            errorCard.hidden=false;
            errorCard.scrollIntoView({behavior:'smooth',block:'nearest'});
          }
        }finally{
          if(submitBtn){submitBtn.disabled=false;submitBtn.removeAttribute('aria-busy');}
          updateFormatLabels();
        }
      });
    }

    const dashExportForm=document.getElementById('export-form');
    if(dashExportForm&&!dashExportForm.dataset.initialized){
      dashExportForm.dataset.initialized='true';
      dashExportForm.addEventListener('submit',function(){
        const btn=dashExportForm.querySelector('button[type="submit"]');
        if(btn){btn.disabled=true;btn.setAttribute('aria-busy','true');}
      });
    }

    // Interactive rating picker.
    const picker=document.querySelector('.star-picker'); const ratingInput=document.getElementById('rating-input'); const ratingLabel=document.getElementById('rating-label');
    if(picker&&ratingInput){
      const stars=[...picker.querySelectorAll('button')]; const labels=['Poor','Fair','Good','Very good','Excellent'];
      let current=parseInt(ratingInput.value||'0',10)||0;
      function paint(value){stars.forEach(function(s,i){s.classList.toggle('selected',i<value)}); if(ratingLabel) ratingLabel.textContent=value?labels[value-1]:'Select a rating';}
      paint(current);
      stars.forEach(function(star,i){
        star.addEventListener('mouseenter',function(){paint(i+1)}); star.addEventListener('mouseleave',function(){paint(current)});
        star.addEventListener('click',function(){current=i+1; ratingInput.value=current; paint(current);});
      });
    }

    // Keep a neutral local vehicle illustration visible until a real vehicle photo loads.
    document.querySelectorAll('[data-review-vehicle-image]').forEach(function(image){
      const fallback=image.parentElement && image.parentElement.querySelector('[data-review-image-fallback]');
      function showFallback(){image.hidden=true;image.removeAttribute('src');if(fallback)fallback.hidden=false;}
      image.addEventListener('error',showFallback,{once:true});
      image.addEventListener('load',function(){image.hidden=false;image.classList.add('is-loaded');if(fallback)fallback.hidden=true;},{once:true});
      if(image.complete){if(image.naturalWidth>0){image.hidden=false;image.classList.add('is-loaded');if(fallback)fallback.hidden=true;}else showFallback();}
    });
    document.querySelectorAll('[data-review-date]').forEach(function(date){
      const parts=(date.textContent||'').trim().split('-').map(Number);
      if(parts.length!==3||parts.some(function(part){return !Number.isFinite(part);})) return;
      date.textContent=new Date(parts[0],parts[1]-1,parts[2]).toLocaleDateString('en-LK',{day:'2-digit',month:'short',year:'numeric'});
    });

    // Character counter.
    const comment=document.querySelector('#review-form textarea[name="comment"]'); const counter=document.getElementById('comment-count');
    if(comment&&counter){const update=()=>counter.textContent=comment.value.length; comment.addEventListener('input',update); update();}

    // Review intelligence refreshes from persisted records every 30 seconds.
    // This updates analytics only; a full page refresh is offered when new review cards arrive.
    const liveToolbar=document.querySelector('[data-review-live-toolbar]');
    if(liveToolbar){
      const endpoint=liveToolbar.dataset.endpoint||'/api/reports/reviews';
      const message=liveToolbar.querySelector('[data-review-live-message]');
      const updated=liveToolbar.querySelector('[data-review-last-updated]');
      const refreshButton=liveToolbar.querySelector('[data-review-refresh]');
      const initialCount=Number(document.getElementById('review-live-total')?.textContent||0);
      let lastKnownCount=initialCount;
      let refreshInProgress=false;
      function setLiveText(id,value){const el=document.getElementById(id);if(el)el.textContent=value;}
      function paintMetric(id,value,formatter){
        const el=document.getElementById(id);if(!el)return;
        if(formatter==='rating')el.textContent=value.toFixed(1)+' / 5';
        else if(formatter==='percent')el.textContent=Math.round(value)+'%';
        else el.textContent=String(value);
      }
      async function refreshReviewInsights(manual){
        if(refreshInProgress||document.visibilityState==='hidden')return;
        refreshInProgress=true;
        if(refreshButton){refreshButton.disabled=true;refreshButton.classList.add('is-refreshing');}
        try{
          const response=await fetch(endpoint,{headers:{'Accept':'application/json'},credentials:'same-origin',cache:'no-store'});
          if(!response.ok)throw new Error('Review insights could not be refreshed.');
          const records=await response.json();
          if(!Array.isArray(records))throw new Error('Unexpected review data.');
          const valid=records.filter(function(r){return Number(r.rating)>=1&&Number(r.rating)<=5;});
          const total=valid.length;
          const average=total?valid.reduce(function(sum,r){return sum+Number(r.rating);},0)/total:0;
          const five=valid.filter(function(r){return Number(r.rating)===5;}).length;
          const attention=valid.filter(function(r){return r.flagged===true||Number(r.rating)<=2;}).length;
          const positive=valid.filter(function(r){return Number(r.rating)>=4;}).length;
          const ratingQuality=average*20;
          const positiveRate=total?positive*100/total:0;
          const attentionRate=total?attention*100/total:0;
          const health=total?Math.round(ratingQuality*.6+positiveRate*.3+(100-attentionRate)*.1):0;
          const healthLabel=!total?'Awaiting feedback':health>=85?'Excellent':health>=70?'Strong':health>=55?'Developing':'Needs attention';
          paintMetric('review-live-average',average,'rating');
          paintMetric('review-live-total',total,'integer');
          paintMetric('review-live-five-star',five,'integer');
          paintMetric('review-live-five-star-percent',total?five*100/total:0,'percent');
          paintMetric('review-live-attention',attention,'integer');
          paintMetric('review-live-health',health,'integer');
          paintMetric('review-live-rating-quality',ratingQuality,'percent');
          paintMetric('review-live-positive-rate',positiveRate,'percent');
          paintMetric('review-live-attention-rate',attentionRate,'percent');
          setLiveText('review-live-health-label',healthLabel);
          const ring=document.querySelector('.experience-health-score');
          if(ring)ring.style.setProperty('--health-score',health+'%');
          const counts=[0,0,0,0,0];
          valid.forEach(function(r){counts[Number(r.rating)-1]++;});
          document.querySelectorAll('[data-review-rating]').forEach(function(row){
            const rating=Number(row.dataset.reviewRating);
            const count=counts[rating-1]||0;
            const countEl=row.querySelector('[data-live-rating-count]');
            const fill=row.querySelector('[data-rating-fill]');
            if(countEl)countEl.textContent=String(count);
            if(fill)fill.style.width=(total?count*100/total:0)+'%';
            row.setAttribute('aria-label','Show '+count+' reviews rated '+rating+' stars');
          });
          const distribution=document.querySelector('[data-review-rating-distribution]');
          if(distribution){
            const note=distribution.closest('.review-intel-panel')?.querySelector('.review-data-note');
            if(note)note.textContent=total+' reviews';
          }
          const attentionCountNode=document.querySelector('.review-kpi-attention');
          if(attentionCountNode)attentionCountNode.classList.toggle('is-live-attention',attention>0);
          const now=new Date();
          if(updated)updated.textContent='Last checked '+now.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit',second:'2-digit'});
          if(total>lastKnownCount){
            if(message)message.textContent=(total-lastKnownCount)+' new review'+(total-lastKnownCount===1?'':'s')+' detected. Refresh the page to load new review cards.';
            liveToolbar.classList.add('has-new-reviews');
          }else if(message){
            message.textContent=manual?'Insights refreshed from saved review records.':'Analytics refresh automatically every 30 seconds.';
            liveToolbar.classList.remove('has-new-reviews');
          }
          lastKnownCount=total;
        }catch(error){
          if(message)message.textContent='Live refresh temporarily unavailable. Displaying the last loaded insights.';
        }finally{
          refreshInProgress=false;
          if(refreshButton){refreshButton.disabled=false;refreshButton.classList.remove('is-refreshing');}
        }
      }
      if(refreshButton)refreshButton.addEventListener('click',function(){refreshReviewInsights(true);});
      refreshReviewInsights(false);
      window.setInterval(function(){refreshReviewInsights(false);},30000);
    }

    // Small success toast auto-dismiss.
    const toast=document.getElementById('server-toast'); if(toast) setTimeout(()=>toast.remove(),4500);
  });
})();
